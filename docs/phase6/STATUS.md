# Phase 6 (Parity audit & QA) — Status, and project close-out

Phase 6 is the last phase in PORT_SPEC.md's plan, and this is the last STATUS.md the project
produces. It does not add content — it audits what Phases 0-5 built, closes the real compile gaps
a whole-tree sweep found, and states plainly how much of PORT_SPEC.md's own "Definition of done"
(§5) this sandbox could and could not satisfy. Where Phase 0-5's STATUS.md files each closed out
one slice of the mod, this one has to be honest about the whole thing at once, including the parts
that are not close to done. That is the point of a QA phase, and this document tries to earn the
same documentation discipline (cite the exact CE line, say what is/isn't verified, don't round a
54% up) that every prior phase's STATUS.md already established.

This phase ran as: a pre-workflow triage pass (this session, before the Phase 6 workflow itself
started — §1), a Stage 1 fix wave that closed the 7 real gaps that triage surfaced (7 `fx*`
tasks — §2), and a Stage 2 audit wave running in parallel with this document (`sy1`'s parity
report, `ca3`'s recipe-graph/reachability audit, `sy2`'s playtest scenario definitions — §3-§5).

## 0. Verification status — read this first

**Nothing in this document, or in any of Phase 6's own deliverables, is compile-verified or
live-verified.** This sandbox cannot run `./gradlew` — independently reconfirmed for this
document specifically:

```
$ curl -sS -o /dev/null -w "maven.neoforged.net -> HTTP %{http_code}\n" https://maven.neoforged.net/releases/net/neoforged/neoforge/
curl: (56) CONNECT tunnel failed, response 403
[agent-proxy] ... maven.neoforged.net:443 — connect_rejected (the egress proxy denied the CONNECT (organization policy) ...)

$ curl -sS -o /dev/null -w "maven.blamejared.com -> HTTP %{http_code}\n" https://maven.blamejared.com/
curl: (56) CONNECT tunnel failed, response 403
[agent-proxy] ... maven.blamejared.com:443 — connect_rejected (the egress proxy denied the CONNECT (organization policy) ...)
```

Both of the build's own dependency repositories are blocked at the egress proxy by what the proxy
itself reports as organization policy, not a transient failure or a fixable misconfiguration. This
sandbox also cannot launch a Minecraft client or server, and cannot run a live CE 1.12.2 dev
instance to dump its registries programmatically. Every claim below is either (a) a direct
citation of something already committed to disk (a file, a diff, a line count), which is checked
and real, or (b) a static-reading conclusion carried over from `sy1`/`ca3`/`sy2`'s own Phase 6
work, which is exactly as reliable as those documents' own stated confidence — this document does
not re-verify their arithmetic, only reads and summarizes it. Say "confirmed by static reading,"
not "works" or "passes," is the standard this document holds itself to, same as every phase before
it.

## 1. Pre-workflow compile triage (this session, before the Phase 6 workflow started)

Before this workflow's own Stage 1/Stage 2 tasks ran, this session did a small, targeted triage of
the "22 pre-existing dangling imports" Phase 5's own review wave found and disclosed
(`docs/phase5/STATUS.md`, "Known gaps": "the project will not compile end-to-end today even with
every Phase 5 gap above closed"). For each dangling import, the triage checked whether anything in
the tree actually implements or consumes it before deciding fix-vs-delete — commit `1747b7e`:

- **12 files deleted** (zero implementors/consumers anywhere in the tree, confirmed by grep — each
  references a class this project already deliberately chose not to port): the 5 legacy
  single-shot ballistics interfaces (`IBulletUpdateBehavior`/`HurtBehavior`/`ImpactBehavior`/
  `RicochetBehavior`/`HitBehavior` — CE's `EntityBulletBase` was intentionally superseded by the
  Sedna/MK4 bullet framework in Phase 3 and never ported), 4 files from an orphaned automation
  slot-monitoring subsystem (`IPneumaticConnector`/`ISlotMonitorProvider`/`SlotMonitor`/
  `StackCache`), 2 orphaned block-interaction interfaces referencing the never-ported 1.7-era
  `ForgeDirection` (`ICrucibleAcceptor`/`IBlowable`), and 1 file belonging to the already-documented,
  deliberately-deferred RBMK crane/control-panel subsystem (`IKeypadHandler`,
  `docs/phase2/rbmk_reactor.md` "Package B").
- **1 import-path typo fixed**: `PylonMediumBlock.java` imported
  `com.hbm.blocks.network.energy.EnergyNetworkBlockEntities` instead of the real
  `com.hbm.blockentity.network.energy.EnergyNetworkBlockEntities` — the target class already
  existed with the exact field this block needed; a plain wrong-package typo, not a missing class.
- **1 tiny annotation added**: `com.hbm.interfaces.SuppressCheckedExceptions`, a small
  source-retention marker annotation `InternalUnsafeWrapper.java` already applied to itself but
  that had never actually been created.

What was left over after this pass — 7 dangling references with **real, load-bearing consumers**
that needed a genuine CE-cross-referenced port, not a quick fix — were handed to this workflow's
Stage 1 as 7 parallel fix tasks (`fx1`-`fx7`). That handoff list, verbatim from the triage commit's
own message, is exactly the 7 classes §2 covers: `EnumSymbol`, `FluidContainerRegistry`, a renamed
storage-crate block, a small `BusAnimation` interface, `ItemAmmo`, `ClimbableRegistry`,
`CalculatorScreen`.

## 2. Stage 1: closing the 7 real remaining compile gaps

This document's own visibility into `fx1`-`fx7`'s work is the same as its visibility into any other
committed change in this tree: the files each task left on disk (all currently staged, uncommitted,
in this session's working tree — `git status` at the time of writing shows 10 new files (9 from
the 7-gap table below plus `client/particle/ParticleAtlasHook.java` from the bonus fix later in
this section) and 19 modified files), read directly, including each new class's own citing-CE javadoc header (this
project's standing per-class convention). What follows is this document's own read of that diff,
not a report `fx1`-`fx7` handed back directly — no separate structured output from those tasks was
available to this document.

| Task (inferred from its class) | New file(s) | Also wired into |
|---|---|---|
| `EnumSymbol` | `render/misc/EnumSymbol.java` (40 lines) | Nothing else — purely additive; unblocks `FluidType`/`Fluids` (already-committed Phase 0 code that referenced these constants since before this class existed) |
| `FluidContainerRegistry` | `inventory/FluidContainerRegistry.java` (318 lines) | `capability/ModCapabilities.java`: `FluidContainerRegistry.register()` now runs first inside `ModCapabilities.register(RegisterCapabilitiesEvent)`, immediately before `NTMFluidCapabilityHandler.initialize()` — that handler's own javadoc requires the registry already populated, and nothing else in this port called it |
| Storage-crate marker interface (`BlockStorageCrate`) | `blocks/generic/BlockStorageCrate.java` (57 lines) | `blocks/machine/CrateBlock.java` now implements it (so `HazardTransformerRadiationContainer`'s `instanceof BlockStorageCrate` check recognizes CE's real crate lineage); `blockentity/IPersistentNBT.java` gained a new default `writeItemComponents(ItemStack)` hook; `blockentity/machine/CrateBlockEntity.java` overrides it to port CE's `TileEntityCrateBase.buildDropData` contained-item radiation sum onto a new `HazardComponents.CRATE_RAD_KEY` data component (CE's raw `cRads` root-NBT double, which lives outside `IPersistentNBT`'s normal NBT_PERSISTENT_KEY tag and so couldn't be carried by `writeNBT` alone) |
| `BusAnimation` (legacy Java animation engine) | `render/anim/BusAnimation.java` (117), `BusAnimationSequence.java` (205), `BusAnimationKeyframe.java` (667) — 989 lines total | `items/IAnimatedItem.java`'s javadoc updated (no implementor is committed in this tree yet — `ItemGrenadeUniversal`/`ItemChainsaw` still defer their own `getAnimation()` bodies as documented client-rendering scope, per that update's own note) |
| `ItemAmmo` | `items/weapon/ItemAmmo.java` (152 lines) | `items/ItemAmmoEnums.java`'s javadoc updated to point at the real class instead of a "does not exist yet" note |
| `ClimbableRegistry` | `handler/ClimbableRegistry.java` (514 lines) | `blockentity/machine/MachineCrystallizerBlockEntity.java` now implements `IClimbable` (previously an explicit "Not ported" stub in that class's own javadoc) — see below |
| `CalculatorScreen` | `inventory/gui/CalculatorScreen.java` (203 lines) | Nothing else — self-contained client-only `Screen`, opened by the already-committed `HbmKeybinds.calculatorKey` |

Total: **9 new files, ≈2,273 lines** summed from the table above (40+318+57+989+152+514+203), plus
the wiring edits above. (The `git status` count in §2's opening paragraph, 10 new files, also
includes `client/particle/ParticleAtlasHook.java` — a 10th new file, 53 lines, introduced by the
bonus fix later in this section, not part of the 7-gap table — bringing that broader total to
≈2,326 lines.) All 7 classes carry this
project's standard CE-citing javadoc header (file path + line count from `upstream/hbm-ce`, or an
explicit note where a class is new port-side scaffolding with no direct 1:1 CE file, as
`BlockStorageCrate`'s marker-interface shape is).

**Two real bugs found and fixed opportunistically while closing these 7 gaps** (beyond the 7 gaps
themselves — both match this project's own standing recurring-bug-pattern list):

- **`hazard/type/IHazardType.java`**: `int hazardRate = RadiationConfig.HAZARD_RATE.get();` was a
  plain interface field initializer — calling a NeoForge `ModConfigSpec` value's `.get()` eagerly at
  the interface's own class-init time, which throws `IllegalStateException` if that happens before
  `ModConfigEvent.Loading` has fired. This is the same category of bug as this project's
  documented eager-`DeferredHolder.get()`-in-a-static-field-initializer rule, just for the config
  subsystem instead of the registry subsystem. Fixed by converting it to a lazy static factory
  method, `static int hazardRate()`, with the 7 implementing hazard-type classes'
  (`HazardTypeAsbestos`/`Blinding`/`Coal`/`Digamma`/`Hot`/`Radiation`/`Toxic`) call sites updated to
  match.
- **`client/particle/ModParticleProviders.java`**: this class is correctly annotated
  `@EventBusSubscriber(..., bus = Bus.MOD)` for its real job (`RegisterParticleProvidersEvent`,
  a mod-bus event) — but that same class-level annotation was also silently disabling its own
  `onTextureAtlasStitched(TextureAtlasStitchedEvent)` handler, since `TextureAtlasStitchedEvent` is
  a real NeoForge **game**-bus event (already correctly identified as such in
  `docs/phase5/custom_particle_types_registry.md`'s own risk section, lines 151-157 — the bug is
  that the finding wasn't acted on until now). This is the mirror image of this project's
  documented "`@EventBusSubscriber` defaults to `Bus.GAME`, mod-bus events need an explicit
  `bus=MOD`" rule: here a class correctly narrowed to `Bus.MOD` for one handler silently broke a
  second, game-bus handler riding along on the same annotation. Fixed by splitting the
  atlas-capture handler out into its own new `client/particle/ParticleAtlasHook.java` (53 lines,
  plain `Bus.GAME`-default class) — no behavior change (nothing reads the captured atlas yet
  either way), but the hook is reachable again for whenever the Content-wave particle-texture work
  this class's own javadoc already anticipates lands.

**What this section cannot claim**: these 7 classes plus the 2 bonus fixes were read carefully
against CE and against this project's own conventions, but **none of it has been compiled**. A
real `javac`/`gradlew compileJava` run against the pinned NeoForge 21.1.228 jars is very likely to
surface at least a few issues this kind of review cannot catch — a wrong-arity method resolving to
an unexpected overload, a generics mismatch, an API shape that drifted from what
`upstream/neo-edition` (this port's only available API-shape reference) showed. Every prior phase's
STATUS.md has made the same disclaimer and been right to; nothing about Phase 6 changes that.

## 3. Parity report headline (`sy1`, `docs/phase6/PARITY_REPORT.md`)

`sy1`'s task was PORT_SPEC.md Phase 6's first deliverable: "dump CE registries... diff against
1.21.1 registries; target >=99% by count with an explicit, justified exclusion list." As that
report's own §0 states, the literal instruction (a runtime dump + automated diff) is not possible
in this sandbox, so it substitutes a **static census**: script-assisted counting of field
declarations, `.register(`/`.put(`/`.add(` call sites, and JSON file counts, on both CE's real
source (`upstream/hbm-ce`) and this port's, across the 8 categories PORT_SPEC.md names.

**Headline: ≈54.2% weighted (count-weighted across all 8 categories), ≈67.7% unweighted
(category average) — both far below the ≥99% target, and not close.**

| Category | CE | This port | % | Confidence |
|---|---:|---:|---:|---|
| Items | ≈3,154 | 2,982 | 94.5% | Approx both sides |
| Blocks | ≈1,165 | 642 | 55.1% | Approx both sides |
| Fluids | 162 | ≈158 | 97.5% | Near-exact both sides |
| Entities | ≈159 | 117 | 73.6% | Approx both sides |
| Sounds (registered ids only) | 381 | 379 | 99.5%* | Near-exact both sides |
| Vanilla-crafting recipes | ≈1,900-2,000 | ≈260 | ≈13.3% | Approx CE, near-exact port |
| Machine recipes | ≈1,718 | 138 | ≈8.0% | Rough CE (regex), near-exact port |
| Advancements | 65 | 65 | 100% | Exact both sides |

\* Sounds' 99.5% is *registered-id* parity only — the playable-audio layer behind those ids is
0/392 (zero `.ogg` files, no `sounds.json` anywhere in this port), a separate and much worse number
the table cell alone does not show.

The two recipe categories (vanilla-crafting and machine recipes) are the reason the weighted total
is as low as it is: they account for 3,668 of CE's ≈8,754 counted entries across all 8 categories
(42%) and are also the two worst-performing categories. This is not a surprise finding — Phase 5's
own STATUS.md already disclosed "CE's real corpus is ~1,900+ recipes... this port now has 261" —
`sy1`'s report is the first place that and the machine-recipe equivalent get rolled into one
count-based percentage.

**The more important number, per `sy1`'s own framing, is not the count percentage at all**: `ca3`'s
companion audit (§4 below) found only ≈12.4% of this port's registered items are reachable by any
in-game path today. The count gap and the reachability gap are "the same underlying shortfall from
two different angles" — the same thin recipe corpora that drag the count percentages down are also
what makes most of the already-registered items unobtainable.

**Exclusion list** (`PARITY_REPORT.md` §4): `sy1` looked specifically for the two exclusion shapes
PORT_SPEC.md names ("dead/deprecated content, 1.12-only mod integrations like GregTech") and found
very little — CE's ≈10-item `weapon_mod_test` debug family (not ported at all, correctly excluded)
is the one real exclusion; CE's GregTech/AE2/OpenComputers references in `Compat.java` turned out
to be entirely CE reacting to *foreign* mods' items, not CE gating its own content behind them, so
that named example does not actually apply. **Total justified exclusions: ≈10 items, <0.2% of the
combined CE census** — comfortably inside PORT_SPEC.md's "<=1%" ceiling, but that is because there
is genuinely little content to exclude, not because the ≈46-point (weighted) shortfall against 99%
was rationalized away. Essentially the entire gap is real, acknowledged, unclosed work, itemized in
that report's own §5 (10 numbered root causes, roughly ordered by impact — the un-ported
Crucible/`MatDistribution` casting system alone is named as the single largest lever).

## 4. Recipe-graph / item-reachability audit headline (`ca3`, `docs/phase6/recipe_graph_audit.md`)

`ca3`'s task was PORT_SPEC.md Phase 6's second deliverable: for every item this port registers, is
it reachable through crafting, machine recipe, mob loot, block loot, world-gen, advancement reward,
or is it a legitimate creative/admin-only design? Same static-reading methodology and the same
"nothing here is compile- or live-verified" caveat as `sy1`.

**Headline: ≈369 of ≈2,982 registered items (≈12.4%) are reachable by at least one path.** Hand
items: ≈301/2,340 (≈12.9%). Block items: ≈68/642 (≈10.6%).

The report's central finding is that this is **not** two thousand-plus scattered individual bugs —
it traces almost entirely to **five confirmed, systemic mechanism gaps**, each already
self-documented somewhere in this port's own code comments (not newly discovered by the audit,
just quantified by it):

1. **CE's Crucible/`MatDistribution` smelting-and-casting system is not ported at all** — the
   single largest cause, blocking ≈700+ items across `MaterialItemGenerator` (188),
   `IngotNuggetItems` (184), `BilletPowderItems` (176), `PlateCrystalWasteItems` (107), and
   `MaterialBlockGenerator` (57 blocks). This port's own `Mats.java` javadoc already says its
   material-entry tables "are simply empty" pending this system.
2. **`ModRecipeProvider` (vanilla crafting datagen) is an explicitly-scoped first slice** — its own
   javadoc names exactly which of CE's 8 crafting sub-registrar classes it covers (tools, part of
   minerals, armor) and which it does not (rods, weapons, consumables, powders, exclusives, the 7
   dynamic `com.hbm.crafting.handlers.*` classes).
3. **CE's block↔ingot 3×3 compression grid is explicitly not attempted** — no material storage
   block (57 of them) can be crafted from or decompressed to its ingot.
4. **No mob/entity loot table datagen exists** — zero `EntityLootSubProvider`/
   `LootContextParamSets.ENTITY` references anywhere in this port.
5. **No world-gen structures exist** — bunkers, radio stations, crashed vertibird, meteor dungeons:
   none are placed (ambient ore/meteorite *placement*, a different and working mechanism, is not
   affected).

`OreBlocks` (the ore/cluster/depth-ore family, ≈68 blocks) is called out as this port's one
genuinely healthy family — real world-gen placement makes essentially all of it reachable, the
exception among otherwise-dark block families.

`ca3` explicitly did not attempt any fix — Phase 6's audit tasks were audit-only by design — and
its own §6 recommendations are the same five-item priority order §3's exclusion-list discussion
already summarized, repeated in this document's §7 handoff list below.

## 5. Playtest scenarios (`sy2`, expected at `docs/phase6/playtest_scenarios.md`)

**As of this document being written, `docs/phase6/playtest_scenarios.md` was not present on
disk** — `sy2` is a Stage 2 task running in the same wave as this one, and per this task's own
brief that concurrency is expected, not a sign anything failed. This document cannot summarize
`sy2`'s actual scenario definitions because they were not readable at write time. What follows is
what `sy2`'s task was scoped to produce, per PORT_SPEC.md's own Phase 6 description, so a reader
knows what to expect and can check it once the file lands:

> "Scripted playtest scenarios: full progression chain (ore -> steel -> assembler -> chemplant ->
> RBMK -> nuke), radiation lifecycle, one full missile launch, gun firing, explosion perf benchmark
> (Tsar-scale under N seconds without watchdog kill)."

**Whatever `sy2` produces, it is authored, not executed, and cannot be anything else in this
sandbox.** Per §0, this environment cannot launch a Minecraft client or dedicated server at all —
there is no runtime here to click through a progression chain, fire a gun, or launch a missile
against. Scripted scenario definitions (step lists, expected states, pass/fail criteria) are the
correct and complete Phase 6 deliverable *given that constraint* — the "playtest scenarios pass on
a dedicated server + client" half of PORT_SPEC.md's Definition of done (§6 below) is a separate,
later action item that requires a human with a real client/server, not something any agent in this
sandbox could ever have satisfied. Whoever picks this project up next should treat
`playtest_scenarios.md` (once confirmed on disk) as a checklist to run by hand or via a real
automated test harness — not as evidence anything has actually been played through, because nothing
has.

**Given the recipe-graph audit's own ≈12.4% reachability finding (§4)**, a reader running these
scenarios for the first time should expect the "full progression chain (ore -> steel -> assembler
-> chemplant -> RBMK -> nuke)" scenario in particular to fail early and often on real survival
acquisition — most of the intermediate materials that chain depends on (billets, plates, ingots
past the ≈95-material `ModRecipeProvider` slice) have no crafting path yet per §4's root cause #1/#2.
This is not a defect in the scenario script; it is the scenario correctly exercising a part of the
mod that is honestly not finished yet.

## 6. PORT_SPEC.md's Definition of done — item by item

PORT_SPEC.md §5, quoted in full:

> "`gradlew build` green; parity report >=99%; all playtest scenarios pass on a dedicated server +
> client; no unregistered assets (datagen verify); exclusion list reviewed and approved by the
> project owner."

Five criteria. Here is where this project actually stands against each one, and exactly what this
sandbox could and could not do to verify it:

1. **`gradlew build` green — NOT MET, and NOT VERIFIABLE from this sandbox at all.** §0 above shows
   the exact, freshly-reconfirmed proxy denial (`CONNECT tunnel failed, response 403`,
   `connect_rejected ... organization policy`) against both `maven.neoforged.net` and
   `maven.blamejared.com` — the build's own dependency resolution cannot even start here. Separately
   from the network block: Phase 5's STATUS.md already disclosed 22 dangling imports that would
   have kept the project from compiling end-to-end even with every other Phase 5 gap closed; this
   phase's own pre-workflow triage (§1) and Stage 1 fix wave (§2) closed all of those, by static
   reading. **"Closed by static reading" is not the same claim as "compiles."** No agent in this
   sandbox has ever run a real compiler against this codebase — not in Phase 0, not here. A real
   `gradlew compileJava` run is the single highest-priority action for whoever has real network
   access (§7 #1).
2. **Parity report >=99% — NOT MET, and independently computed rather than assumed.** `sy1`'s
   static census (§3) puts this at ≈54.2% weighted / ≈67.7% unweighted — both far below target, and
   the report is explicit that this is a real, substantially-unclosed gap, not an artifact of
   counting methodology (except for the items category specifically, where CE's 1.12
   metadata-sub-item convention structurally understates CE's true content surface relative to this
   port's 1.21 one-id-per-variant convention — §3.1 of that report flags this explicitly rather than
   letting the 94.5% items figure read as more reassuring than it is). The report is a static
   census, not the literal "dump CE registries... diff" PORT_SPEC.md describes, because this
   sandbox cannot run either registry live.
3. **All playtest scenarios pass on a dedicated server + client — NOT MET, and could never have
   been met in this sandbox.** §5 above: no client/server launch capability exists here at all.
   `sy2`'s scenarios (once on disk) are authored and ready to run, but "authored" and "passing" are
   different claims, and only a human with a real Minecraft install can produce the second one.
4. **No unregistered assets (datagen verify) — PARTIALLY ADDRESSED, with an important caveat.**
   Phase 1's and Phase 5's own research already established, by direct code read (not by running
   datagen — that also needs `gradlew`), that this port's model/blockstate/loot-table/lang datagen
   providers are **structurally exhaustive**: `ModItemModelProvider`, `ModBlockStateProvider`,
   `ModBlockLootTableProvider`, and `ModLanguageProvider` all iterate the *live* `DeferredRegister`
   (`ModItems.ITEMS.getEntries().forEach(...)`, same pattern for blocks) rather than a hardcoded id
   list, with an opt-out interface (`ICustomItemModelRegister`/`ICustomBlockModelRegister`) for
   anything needing a non-default model — `docs/phase5/advancement_and_recipe_datagen_assets.md`
   §2.2's own words: "There is no possible 'registered but has no model/blockstate/loot/lang JSON'
   state in this port's current architecture." That answers the letter of "no unregistered assets"
   as far as static reading can. **It does not mean the assets are correct or complete**: that same
   report confirms **zero PNG texture files** exist anywhere in this port's resource tree today
   (`find ... -name '*.png' | wc -l` → `0`) against CE's real **6,965** PNGs — independently
   confirmed by four other Phase 5 reports — plus the sound-asset gap `sy1`'s parity report adds
   (0/392 `.ogg` files). Every registered item/block will get *some* valid, non-crashing generated
   JSON the moment `runData` actually executes — but a real `runData` run has never happened here
   (no `gradlew`), so even this claim is "confirmed exhaustive by reading the provider code," not
   "confirmed by running datagen and checking the output," and everything that JSON references
   (the actual texture/model files) is still missing.
5. **Exclusion list reviewed and approved by the project owner — NOT MET, and cannot be met by any
   agent.** `sy1` produced a draft exclusion list (§3 above, `PARITY_REPORT.md` §4) — ≈10 items,
   comfortably under the 1% ceiling. It has not been reviewed or approved by a human project owner,
   because no such review has happened in this sandbox; this is explicitly a human sign-off step,
   not a technical one, and the most honest thing this document can do is flag it as outstanding
   rather than imply an agent's own audit counts as "approval."

**Net: 0 of 5 Definition-of-done criteria are met.** One (#4) is structurally set up to be met once
a real `runData` confirms it and the asset gap closes; the rest require either real network/build
access this sandbox does not have, a live client/server this sandbox does not have, or an actual
human decision-maker this sandbox is not.

## 7. Handoff: what a human with real network/build access should do next, in priority order

1. **Get real network access to `maven.neoforged.net`/`maven.blamejared.com` (or a mirrored/cached
   dependency set) and run `gradlew compileJava`, then `gradlew build`.** This is unambiguously the
   first thing to do — it is the one Definition-of-done criterion this document could not even
   attempt, and every other criterion downstream of it (playtest execution, a trustworthy live
   parity dump) depends on it working first. **Expect real compile errors that this six-phase
   static-reading process could not catch** — every phase's STATUS.md has said as much, and Phase
   6's own Stage 1 fix wave (§2) already found two live bugs (an eager-config-`.get()` interface
   field, a mis-scoped `@EventBusSubscriber` handler) that no earlier phase's static review caught
   despite those exact bug *patterns* being named ground rules from Phase 0 onward — a real compiler
   will very likely find more, and possibly some that are not simple typos.
2. **Once it compiles, run `sy2`'s playtest scenarios** (`docs/phase6/playtest_scenarios.md`, once
   confirmed present) on a real dedicated server + client. Expect the full-progression-chain
   scenario specifically to expose the ≈12.4% reachability gap (§4/§5) hard and early — that is not
   a scenario-script bug, it is the mod's real current state.
3. **Bulk texture/model/sound asset migration.** Named as the single largest, most repeatedly-flagged
   cross-cutting gap across this entire project — Phase 5's STATUS.md ("no Phase 5 area claimed
   ownership of the bulk copy... the single largest blocker to any of Phase 5's rendering work being
   visually verifiable"), `advancement_and_recipe_datagen_assets.md` §2.2 (independently confirmed
   by 4 other Phase 5 reports), and this phase's own parity report (§3, sounds: 0/392) all name it.
   CE ships ≈6,965 PNG/OBJ files and 392 `.ogg`/sound entries; this port ships a handful of overlay
   PNGs and gun-animation JSONs. Every renderer, block model, and sound id this whole project wrote
   is already pointed at the *correct* expected resource path — this is a bulk-copy-and-verify task,
   not new design work, which is exactly why it is high-leverage: closing it would make the other
   five phases' rendering/audio work visually and audibly checkable for the first time.
4. **Close the two largest parity/reachability levers**, per `sy1`/`ca3`'s converging
   recommendations: (a) port a minimal Crucible + `MatDistribution` casting system — the single
   largest connected cluster of currently-unreachable items (≈700+, §4 root cause #1); (b) extend
   `ModRecipeProvider` to CE's remaining crafting-recipe classes (`RodRecipes`/`WeaponRecipes`/
   `ConsumableRecipes`/`PowderRecipes`/`ExclusiveRecipes`) and port recipe data for CE's remaining
   ≈63 (of 72) machine-recipe classes — together these are the largest raw-count levers on the
   ≈54.2% weighted parity number (§3).
5. **Review and approve (or amend) the exclusion list** (`PARITY_REPORT.md` §4) — a project-owner
   decision this document explicitly cannot make on anyone's behalf.
6. **Smaller, still-real follow-ups**, roughly in the order `ca3`'s own audit recommends them: the
   block↔ingot 3×3 compression grid (mechanical, high count-per-effort once #4a exists), a minimal
   `EntityLootSubProvider`, and world-gen structure placement (bunkers/radio stations/crashed
   vertibird/meteor dungeons) — all currently hard zeros per §4.
7. **Once #1-#3 are done, re-run the parity census live** — a real `gradlew` run unlocks dumping
   actual registered-entry counts from a running game instance instead of `sy1`/`ca3`'s
   necessarily-approximate static counts, which would finally let PORT_SPEC.md's literal "dump CE
   registries... diff against 1.21.1 registries" instruction be carried out as written, on both
   sides, for a trustworthy final number.

## 8. Closing note

Six phases, 52 commits, ≈1,663 Java files and ≈182,900 lines of Java in this port (against CE's own
≈3,933 Java files) — every one of those files carrying this project's standing convention of citing
the exact CE source it was read from. That discipline is the main thing this project can vouch for
without a compiler: every class in this tree was written by someone who actually opened the
corresponding CE file and read it, not by inference or pattern-matching against a mod's general
shape. What this project cannot vouch for, and has tried not to pretend otherwise about at any
point from Phase 0 through here, is that any of it actually runs. `gradlew` has not gone green once
across this entire effort — not because the code is known-broken, but because the tool that would
tell us either way has been unreachable from inside this sandbox from the very first phase, and
every STATUS.md since has said so plainly rather than let a green-looking document stand in for a
green build. This document does not change that. The honest summary of where this project stands:
a large, carefully-sourced, never-compiled 1.21.1 skeleton of CE, roughly half-populated by count
and about one-eighth reachable in survival, with a precise, itemized map of exactly what is missing
and in what order to close it — handed off complete, not handed off finished.
