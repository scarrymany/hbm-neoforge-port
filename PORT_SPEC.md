# HBM's Nuclear Tech CE: 1.12.2 Forge -> 1.21.1 NeoForge Port Specification

## 0. Repositories

- **Source of truth (what we port):** HBM's Nuclear Tech - Community Edition, MC 1.12.2 Forge - https://github.com/Warfactory-Official/Hbm-s-Nuclear-Tech-CE (modid `hbm`, v2.5.0.5, default branch; clone this and read its code directly for every port task).
- **Existing partial 1.21.1 port (reference only):** HBM's NTM Neo Edition - https://github.com/ohiomannnn/HBMsNTM-NEO-EDITION (MC 1.21.1, NeoForge 21.1.228+; use for NeoForge registration/rendering patterns, never as the content/logic source). Published builds: https://modrinth.com/mod/hbms-nuclear-tech-ne
- **Upstream original (historical context only, do not port from it):** https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT

## 1. Objective

Port **HBM's Nuclear Tech - Community Edition** (modid `hbm`, v2.5.0.5, MC 1.12.2, repo `Warfactory-Official/Hbm-s-Nuclear-Tech-CE`) to **Minecraft 1.21.1 / NeoForge 21.1.x** with **99% feature parity**. The existing partial port `ohiomannnn/HBMsNTM-NEO-EDITION` (1.21.1, NeoForge 21.1.228+) is a **reference only** for registration/rendering patterns; the source of truth for content and logic is CE. Fresh worlds only - no save migration.

## 2. Ground rules (all agents, all phases)

- Java 21, Mojang official mappings, NeoForge `DeferredRegister` for every registry. Preserve registry ids (`hbm:<name>`) exactly as in 1.12.2 wherever the id is legal in 1.21 (lowercase, no invalid chars); keep a rename map for exceptions.
- Preserve package layout `com.hbm.*` one-to-one where feasible so cross-referencing the old code stays mechanical.
- ItemStack NBT -> **Data Components** (with codecs + stream codecs). Document every NBT key -> component mapping in `docs/nbt-components.md`.
- Networking: `CustomPacketPayload` + `StreamCodec`, registered via `RegisterPayloadHandlersEvent`. Port every packet in `com.hbm.packet`.
- Capabilities: NeoForge block/item/entity capabilities for item, fluid, and the mod's own **HE energy** system. Keep HE as a custom capability (do NOT collapse into FE); optionally expose an FE bridge behind a config flag.
- Assets: copy textures/sounds/lang from CE verbatim; regenerate models, blockstates, recipes, tags, loot tables, advancements through **datagen**. No placeholder assets, no TODO stubs - every registered object ships with model, lang entry, and (where applicable) recipe.
- Hardcoded 1.12 recipes/machine recipes -> JSON `Recipe<?>` types with serializers; write a one-off extraction script that dumps CE's recipe registrations to JSON so agents convert data, not by hand.
- World gen: ores/features via datapack features + `BiomeModifier`; structures via structure templates or code-driven placement matching CE behavior.
- Radiation, pollution, and other per-chunk state -> NeoForge **Data Attachments** on chunks/entities + `SavedData` for world-level systems.
- GUIs: `AbstractContainerMenu` + `Screen` pairs; potions -> `MobEffect`; damage sources -> `DamageType` datapack entries; config -> NeoForge config (TOML), preserving CE config option names in comments.
- CE mixins: audit each; re-target to NeoForge/vanilla 1.21.1 only if the hook has no event equivalent, otherwise replace with events.
- Every agent deliverable must compile (`gradlew build` green) and be self-registered end-to-end. CI gate on every merge.

## 3. Phase plan and multi-agent orchestration

**Mandatory orchestration rule: every phase runs on 15 parallel agents.** Each phase executes in three waves, all 15-agent:

1. **Research wave** - before writing code for a phase, fan out 15 read-only research agents over the CE codebase (and, where relevant, the Neo Edition reference) to map the packages that phase touches: class inventory, dependencies, NBT keys, packets, render hooks, GUI list, recipe registrations. Output: one structured research report per agent, merged into the phase work-package breakdown.
2. **Implementation wave** - 15 coding agents, one per work package from the breakdown. Packages must be dependency-independent within the wave; anything shared (base classes, APIs) goes into an earlier wave or Phase 0.
3. **Review wave** - 15 reviewer agents re-read the diff of the implementation wave against the CE original (logic parity, missing registrations, missing assets, missed NBT keys), then fix agents apply confirmed findings.

The per-phase package lists below are the default decomposition for the implementation wave; the research wave may rebalance them (split a heavy package, merge trivial ones) as long as the wave stays at 15 agents. Phases are sequential; packages inside a wave are independent.

**Phase 0 - Foundation.** Gradle/NeoForge skeleton, mod entrypoint, creative tabs, base registries, `lib`/`util` port, material system (`NTMMaterial`/Mats), fluid registry (CE has 100+ custom fluids; port as NeoForge fluids with types, most without world blocks), HE energy capability + network graph, hazard system registry (radiation/hot/blinding/explosive item hazards), sound events, keybinds, config, packet infrastructure, damage types, base item/block classes. Nothing content-heavy compiles without this, so Phase 0 quality gates everything.

**Phase 1 - Content mass.** All items (~thousands: ingots, powders, plates, wires, crystals, parts, fuels, waste, templates/stamps) and all simple blocks (ores across stone/nether/end variants, decorative, structural, machine casings), split by category into packages. Datagen for all models/tags/loot. Hazard bindings for every radioactive/hot item. Creative tab population matching CE ordering.

**Phase 2 - Machines.** Block entities + menus + logic: power generation (burner engines, RTGs, turbines, diesel/gas generators, solar), reactors (breeding reactor, **RBMK multiblock with all column types and melt-down logic**, PWR, fusion reactor, Watz), processing (shredder, assembler, chemical plant, crystallizer, centrifuge, gas centrifuge, cyclotron, SILEX/laser isotope separation, electrolyser, mixer), storage (mass storage crates, fluid barrels/tanks, batteries/energy storage), logistics (cables + energy net, fluid ducts, item conveyors/crane inserters), oil chain (derrick, pumpjack, refinery, fracking). Multiblock framework first (one package), then machines fan out.

**Phase 3 - Weapons & destruction.** Explosion engine port (the mk4/mk5 explosion algorithms, chunk-batched block removal for performance, fallout generation), all bombs and detonators, missiles + launch pads/silos + designators, turrets, the CE gun framework (guns, ammo, reload/recoil/animation via `animloader`), grenades, melee/tools, armor sets + FSB armor modifier system + hazmat protection integration.

**Phase 4 - World & simulation systems.** Chunk radiation system (storage, spread, decay, entity irradiation, Geiger feedback), pollution system, fallout rain/effects, contamination effects (`potion` port), world gen structures (bunkers, radio stations, crashed vertibird, oil wells, meteor dungeons), meteor events, ore veins/bedrock ores, satellites (launch, orbital scans/miners), custom entities (creepers variants, bosses, projectiles, vehicles if present in CE 2.5.0.5).

**Phase 5 - Client & UX.** All BERs/ item renderers (port OBJ models via NeoForge OBJ loader; custom loaders only where CE uses bespoke formats), instanced particle engine (`particle_instanced`) rewritten on modern render pipeline with a shader-compat fallback path, HUD overlays (Geiger counter, armor/gun HUD), all ~100+ GUI screens' visual parity, JEI integration for every recipe type, sound wiring, full lang file port (en_us mandatory; ru_ru and others copied), main-hand animations.

**Phase 6 - Parity audit & QA.** Automated parity report: dump CE registries (items, blocks, fluids, recipes, entities, sounds) from a 1.12.2 dev instance to JSON; diff against 1.21.1 registries; target >=99% by count with an explicit, justified exclusion list (<=1%: dead/deprecated content, 1.12-only mod integrations like GregTech). Recipe-graph audit (every item reachable). Scripted playtest scenarios: full progression chain (ore -> steel -> assembler -> chemplant -> RBMK -> nuke), radiation lifecycle, one full missile launch, gun firing, explosion perf benchmark (Tsar-scale under N seconds without watchdog kill). Fix waves run as review->fix agent pairs.

## 4. Cross-cutting risks

1. **Rendering** is the largest rewrite surface (GL1-era TESRs, instanced particles, `animloader`); budget Phase 5 accordingly and prototype the particle pipeline during Phase 0.
2. **Explosion performance** on 1.21 chunk system - use batched `LevelChunk` section writes + deferred lighting.
3. **RBMK and gun framework** are the deepest logic packages - assign strongest agents, port with unit tests on the pure-logic cores (neutron flux math, ballistics).
4. CE targets Java 8 bytecode via JvmDowngrader - source reads as modern-ish Java; do not cargo-cult its build hacks.
5. License: GPLv3/LGPLv3 - the port must keep the license and attribution; CE's `processor/` dir is All Rights Reserved - do not copy it.

## 5. Definition of done

`gradlew build` green; parity report >=99%; all playtest scenarios pass on a dedicated server + client; no unregistered assets (datagen verify); exclusion list reviewed and approved by the project owner.
