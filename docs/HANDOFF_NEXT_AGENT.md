# Handoff brief — HBM's Nuclear Tech CE → NeoForge 1.21.1 port

Paste this whole file as your first prompt to a new agent/session. It has everything needed to
continue without re-discovering context.

## Repository

- **Working repo:** `scarrymany/hbm-neoforge-port` (GitHub).
- **Dev branch (do all work here):** `claude/project-port-phases-1-6-ohyvkm`.
- **Integration branch:** `master` — fast-forward it from the dev branch at safe checkpoints only
  (verify with a merge-base check first; never force-push).
- An open PR already exists: https://github.com/scarrymany/hbm-neoforge-port/pull/1.
- Full spec: `PORT_SPEC.md` in the repo root — read it first, it is short (62 lines) and is the
  actual contract for this project.

## The task

Port **HBM's Nuclear Tech: Community Edition** (CE), a large Minecraft 1.12.2 Forge mod
(`Warfactory-Official/Hbm-s-Nuclear-Tech-CE`), to **Minecraft 1.21.1 / NeoForge 21.1.x**, targeting
**99% feature parity** — this is explicitly a *full* mod port (all items, blocks, machines, weapons,
reactors, world-gen, entities, rendering, GUIs, recipes, assets), not a subset. Two reference repos
matter and must not be confused:

- `upstream/hbm-ce` (cloned locally) — CE's real 1.12.2 source. **Sole source of truth for content,
  numbers, and behavior.** Every ported class should cite the CE file/line it came from.
- `upstream/neo-edition` (cloned locally) — a separate, much smaller, but real and *compiling*
  NeoForge 1.21.1 mod pinned to the exact same NeoForge version (`21.1.228`) this project targets.
  **Reference only for real 1.21.1 API shapes/registration patterns** — never port behavior or
  numbers from it.

Definition of done (`PORT_SPEC.md` §5): `gradlew build` green; parity report ≥99%; all playtest
scenarios pass on a dedicated server + client; no unregistered assets; exclusion list approved.

## Methodology

`PORT_SPEC.md` mandates a phase plan (Phase 0 Foundation → Phase 1 Content mass → Phase 2 Machines
→ Phase 3 Weapons/destruction → Phase 4 World/simulation → Phase 5 Client/UX → Phase 6 Parity
audit/QA), each phase run as parallel-agent waves (research → implement → review → fix) via the
`Workflow` tool. Phases 0–6 are done (see `docs/phase*/STATUS.md` for each phase's honest
close-out). Work has since continued past Phase 6 toward 99% parity with additional phases:
Phase 7 (Crucible machine + full crafting/machine recipe corpus — done, committed) and planned
Phase 8 (remaining blocks/loot tables/world-gen structures), Phase 9 (remaining entities), Phase 10
(bulk texture/model/sound asset migration), Phase 11 (final whole-tree compile triage + re-run
parity report) — **none of Phase 8–11 has started yet.**

## Hard sandbox constraint — read this before doing anything

**This coding sandbox cannot reach `maven.neoforged.net` or `maven.blamejared.com`** (HTTP 403 at
the egress proxy, confirmed repeatedly as organization policy, not transient). That means **no
agent working in this sandbox can ever run `./gradlew`** — not to compile, not to run datagen, not
to boot a client. All work here is static source editing, verified only by reading code and cross-
checking against `upstream/neo-edition`'s real compiling equivalents. Real compiler feedback comes
from a **separate, network-unrestricted testing agent** the project owner runs independently; that
agent's job is strictly to run `./gradlew compileJava`/`build`/`runData` and report raw errors —
never to fix anything itself (see `docs/phase6/BUILD_VERIFICATION_HANDOFF.md` for its exact
instructions if you need to request another verification pass).

## Current real state (as of the latest build-error report, v2)

Two build-error reports have come back from the external testing agent so far:

- **v1** (commit `655f63c`, capped by javac's default 100-error limit): 200 errors / 142 warnings /
  108 files.
- **v2** (commit `9dd5beb`, uncapped via `-Xmaxerrs 10000`): **303 errors / 71 warnings / 162
  files.** This is the authoritative, current picture — v1 was an undercount.

A first fix wave (commit `9dd5beb`, "Fix wave 2/2") closed the errors v1 explicitly listed
(particle `quadSize` API change, a handful of `codec()` overrides, stray imports, an access
transformer for `Player#hurtDir`), but **did not do its own repo-wide search for the same
patterns**, so it left the bulk of each cluster unfixed. v2 exposes the true scope. **A second,
properly-scoped fix wave was diagnosed but not yet launched or applied** — the diagnostic work
below is the immediate next step.

### v2 error clusters, by size (highest priority first)

1. **`codec()` override missing — 206 errors / 103 block classes (by far the largest cluster).**
   NeoForge 1.21.1's `BaseEntityBlock`/`FallingBlock` requires a real
   `MapCodec<? extends X> codec()` override. Prior session work traced the full class hierarchy and
   found ~9 natural family groupings sharing abstract intermediate base classes — fixing the shared
   base per family (not each of the ~115 concrete leaf classes individually) is the efficient path:
   `BlockChargeBase` (4 subclasses), `NukeCasingBlockBase` (10), `PylonBaseBlock` (1),
   `FluidDuctBaseBlock` (9), `BlockDummyable` (28 direct + nested families `RBMKBaseBlock`/10,
   `RBMKControlBlock`/2, `TurretBaseNTBlock`/9), plus ~32 files that extend
   `BaseEntityBlock`/`FallingBlock` directly with no shared intermediate. `CrashedBombBlock.java`
   was spot-checked and confirmed to belong to this cluster (the v2 report's own citation for it,
   a missing-import error, is stale — that part is already fixed; it still needs `codec()`).
2. **Illegal forward reference — 50 errors / 2 files:** `BedrockOreGrade.java` and
   `MultitoolPassiveItems.java` — Java enum constants referencing a static field declared later in
   the same enum body. Not yet investigated in depth; likely needs the referenced constants
   reordered or extracted to a separate holder class.
3. **Missing import: `EnergyNetworkBlockEntities` — 36 errors across ~12 files.** Real class is at
   `com.hbm.blockentity.network.energy.EnergyNetworkBlockEntities`; this exact bug was already fixed
   once for `PylonMediumBlock.java` in Phase 6. The remaining files (enumerated by grep, not yet
   fixed) are: `BlockCable.java`, `PylonRedWireBlock.java`, `CableDiodeBlock.java`,
   `PylonLargeBlock.java`, `PowerCableBoxBlock.java`, `EnergyNetworkBlocks.java`,
   `SubstationBlock.java`, `CableDetectorBlock.java`, `CableSwitchBlock.java` — a one-line import
   addition each.
4. **Missing import: `BombBlockEntities` — 12 errors / ~6 files.** Real class is at
   `com.hbm.blockentity.bomb.BombBlockEntities`. Files confirmed missing the import:
   `LaunchPad.java`, `LaunchPadRusted.java`, `LaunchPadLarge.java` (same one-line fix).
5. **Invalid constructor reference — 22 errors / 3 files:** `ModDataGenerators.java`,
   `MachineItems.java`, `ClientModRegistry.java` — not yet investigated.
6. **"Method does not override" — 32 errors,** scattered (examples: `RBMKCoolerBlockEntity`,
   `BlockTNTBase`, `BatteryBlock`) — likely stale `@Override` annotations on methods NeoForge
   1.21.1 renamed/removed; not yet investigated beyond the named samples.

### Known recurring bug patterns (apply these whenever you see the symptom, don't re-derive)

- A `DeferredHolder`/registry-holder `.get()` call inside a **static field initializer** crashes at
  class-load time if that runs before the relevant `RegisterEvent` fires. Fix: convert to a lazy
  cached static factory *method* instead of an eager `static final` field (worked example:
  `BlockModDoor.HAND_OPENABLE_METAL()` in `src/main/java/com/hbm/blocks/generic/BlockModDoor.java`).
  Same applies to NeoForge config values (`ModConfigSpec#get()`) — see `IHazardType.hazardRate()`.
- `@EventBusSubscriber` defaults `bus()` to `Bus.GAME`; it does **not** auto-detect
  `IModBusEvent` — anything listening for a mod-bus event (e.g. `RegisterEvent`,
  `RegisterParticleProvidersEvent`) needs explicit `bus = EventBusSubscriber.Bus.MOD`.
- NeoForge Access Transformers (`src/main/resources/META-INF/accesstransformer.cfg`) work and are
  already used once (widening `Player#hurtDir`) — use this instead of reflection when a vanilla
  field/method just needs wider visibility.

## Immediate next steps for whoever picks this up

1. Finish the scope-discovery for the not-yet-investigated v2 clusters (forward-reference, invalid
   constructor refs, method-does-not-override) the same way the codec()/import clusters were
   scoped: grep the whole repo for the pattern yourself, don't trust the report's sample file list
   as exhaustive (that mistake caused the first fix wave to undercount by 100+ errors).
2. Apply the two already-fully-enumerated, purely mechanical import fixes directly (12 files
   total) — no agent wave needed.
3. Design and run a second fix-wave `Workflow` (likely ~9 agents for the codec() families, one each
   for the remaining clusters, plus a review pass), having each agent do its own repo-wide grep
   within its assigned scope rather than working off a truncated list.
4. Commit, push to the dev branch, then request another `BUILD_ERRORS.md` pass from the external
   testing agent to confirm the real error count is dropping.
5. Once compileJava is clean, resume Phases 8–11 (remaining blocks/loot tables/world-gen,
   remaining entities, bulk asset migration, final parity report) using the same
   research→implement→review→fix wave methodology.
