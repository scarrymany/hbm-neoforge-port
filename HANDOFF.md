# Handoff: HBM's Nuclear Tech CE -> NeoForge port

Read this file FIRST, completely, before touching any code. It exists so you (a fresh agent
with no memory of the prior session) can continue this port autonomously, without asking the
user clarifying questions - the user is unreachable while this work continues. Use your own
engineering judgment for anything not covered here, note the decision in a doc, and keep going.

## What this project is

A from-scratch port of "Hbm's Nuclear Tech - Community Edition" (Minecraft 1.12.2/Forge,
modid `hbm`) to Minecraft 1.21.1/NeoForge 21.1.228, Java 21, Mojang mappings, targeting 99%
feature parity. Full specification: [PORT_SPEC.md](PORT_SPEC.md) - read it in full now, it
defines the 7 phases (0 Foundation, 1 Content mass, 2 Machines, 3 Weapons & destruction,
4 World & simulation, 5 Client & UX, 6 Parity audit & QA), the mandatory multi-agent
orchestration rule (every phase runs as a 15-agent research/implement/review wave), and the
cross-cutting ground rules (DeferredRegister, Data Components instead of NBT, NeoForge
capabilities, datagen instead of hand-written assets, license/attribution).

License: GPLv3/LGPLv3 (see [LICENSE](LICENSE)/[LICENSE.LESSER](LICENSE.LESSER)/[NOTICE](NOTICE)) -
this is a hard requirement carried over from the source project, keep it.

## Environment setup (do this first, every fresh clone/sandbox)

1. **JDK 21 for running Gradle itself.** Whatever JDK is preinstalled in your sandbox may be
   newer (Gradle 8.14.3, pinned by the wrapper, does not support running on JDK 25+ - fails
   with "Unsupported class file major version 69"). Install a JDK 21 (e.g.
   `apt-get install -y openjdk-21-jdk` on Debian/Ubuntu sandboxes, or the platform equivalent)
   and point Gradle at it via `gradle.properties`' `org.gradle.java.home` (already set in this
   repo to a Windows path from the original session - **update that path** for your OS/sandbox,
   or remove the line and set `JAVA_HOME` in the shell environment instead). The NeoForge
   toolchain itself (compilation target) is already correctly pinned to Java 21 in
   `build.gradle` via `java.toolchain.languageVersion` - only the Gradle-daemon-launcher JDK is
   the thing you need to fix per-machine.
2. **Re-clone the two reference repos** (gitignored, not part of this repo, needed for every
   porting task):
   ```bash
   mkdir -p upstream
   git clone --depth 1 https://github.com/Warfactory-Official/Hbm-s-Nuclear-Tech-CE.git upstream/hbm-ce
   git clone --depth 1 https://github.com/ohiomannnn/HBMsNTM-NEO-EDITION.git upstream/neo-edition
   ```
   `upstream/hbm-ce` is the 1.12.2 CE source of truth - the thing you are porting FROM, read
   its code directly for every task. `upstream/neo-edition` is a partial existing 1.21.1
   NeoForge port - reference ONLY for confirming real, working NeoForge 21.1 API shapes and
   registration/rendering patterns, NEVER as a source of content or business logic (it is
   incomplete and sometimes simplifies things CE does more richly - Phase 0/1 work already
   caught cases where copying its simplifications instead of CE's real behavior would have been
   wrong; when in doubt, CE is ground truth for behavior, Neo Edition is just an API-shape
   sanity check).
3. Verify the skeleton still builds before doing anything else:
   ```bash
   ./gradlew compileJava --console=plain
   ```
   As of this handoff, expect **roughly 100 errors** - this is not a fresh regression. Read
   [docs/phase0/STATUS.md](docs/phase0/STATUS.md) for the full triage: essentially all of them
   are forward references to content that belongs to a later phase (Phase 2 machines/multiblock,
   Phase 3 weapons/armor, Phase 4 radiation/pollution) and are expected to close as those phases
   land. Before treating any compile error as a real bug, check whether it's already accounted
   for in `docs/phase0/STATUS.md` or `docs/phase1/*.md` - if not, it may be new and worth fixing.
   Do NOT run `gradlew build`/`compileJava` concurrently from multiple agents in the same
   working tree - it corrupts the shared build directory. Only one build at a time, run by
   whoever is doing final integration/verification.

## Current status - read this carefully, it is mid-flight

**Phase 0 (Foundation): DONE and stable.** Committed in full. 15 areas (lib/util, material
system, fluid types, HE energy API + network graph, hazard registry, sound registry, config,
packet framework, capability framework, empty ModItems/ModBlocks/ModCreativeTabs registries,
HBM API/interfaces, main registry/keybinds, damage types) all went through research -> implement
-> review -> fix. Full record: [docs/phase0/DIGEST.md](docs/phase0/DIGEST.md) (detailed,
per-area) and [docs/phase0/STATUS.md](docs/phase0/STATUS.md) (summary, known gaps, open
decisions). `MainRegistry.java`/`CommonEvents.java` are wired and working.

**Phase 1 (Content mass): IN PROGRESS, INTERRUPTED MID-WAVE - needs verification, not a
restart from scratch.**

A 12-agent research wave already completed and is fully trustworthy:
[docs/phase1/*.md](docs/phase1/) (one report per area) plus two synthesis files,
[docs/phase1/DIGEST_REMAINDER.md](docs/phase1/DIGEST_REMAINDER.md) (digest of 10 of the reports)
and this session's own reading of `moditems_generative.md`/`modblocks_generative.md` in full.
**Read every file in `docs/phase1/` before writing any Phase 1 code** - they contain exact CE
class names, variant counts, confirmed NeoForge API decisions, and explicit Phase-1-safe vs
deferred-to-later-phase boundaries that took significant research to establish. Do not
re-derive this from scratch.

Based on that research, a 15-area **implementation wave** (implement -> review -> fix per area)
was launched twice: once ran ~7 of ~45 possible agent calls before the process died
uncommanded; it was resumed from cache and ran further, then was deliberately stopped
(not crashed) so this handoff could be written and the repo pushed. **The stop means most
areas' REVIEW and FIX stages did not run** - only the IMPLEMENT stage got real work done for
most areas, and that work has NOT been independently audited against CE. Treat every file
below as "written, not yet verified" unless a specific note says otherwise.

The 15 planned areas and their file-presence status at handoff time (verify each yourself -
this is a file-existence check, not a correctness check):

| # | Area | Key deliverable(s) | Status at handoff |
|---|---|---|---|
| 1 | `datagen_framework` | `com.hbm.datagen.*`, `com.hbm.items.datagen.*`, `com.hbm.blocks.datagen.*`, `ICustomItemModelRegister`/`ICustomBlockModelRegister` | Files present (7ish), **not reviewed** |
| 2 | `creative_tabs_infra` | `com.hbm.creativetabs.CreativeTabContents`, `ModCreativeTabs` wiring | **Implement + review both completed, review found nothing to fix.** Most trustworthy Phase 1 area. |
| 3 | `items_tool` | `com.hbm.handler.ability.*` (6 files) + `com.hbm.items.tool.*` (14 files) | Ability framework looks reasonably complete; tool item list is a PARTIAL subset of the ~46-file research plan - many files from `items_tool.md`'s "(a) genuine Phase 1 tools" list are likely still missing. Verify against the report and finish it. |
| 4 | `moditems_autogen` | `com.hbm.items.MaterialItemGenerator` (17-shape loop, 188 variants) | **Implement + review + fix all completed.** Trustworthy. |
| 5 | `modblocks_generative` | `com.hbm.blocks.OreBlocks` + `com.hbm.blocks.generic.*` ore classes | Present but likely only covers part of the plan (ore/cluster/depth family confirmed; verify the ~55-57 material storage-block family with its per-material behavior lookup actually landed - it may not have). |
| 6 | `items_ingot_nugget` | `com.hbm.items.IngotNuggetItems` (~174 fields) | Implemented (327 lines), **not reviewed**. |
| 7 | `items_billet_powder` | `com.hbm.items.BilletPowderItems` (~176 fields) | Implemented (383 lines), **not reviewed**. |
| 8 | `items_plate_crystal_waste` | `com.hbm.items.PlateCrystalWasteItems` (~100 fields) | **Implement + review both completed, review found nothing to fix.** Trustworthy. |
| 9 | `items_food_gear` | `com.hbm.items.food.*`, `com.hbm.items.gear.*` | **Largely INCOMPLETE.** `items/food` has only `ItemLemon.java` - the other 6 clean food items, the 4 metadata-flatten classes (ItemConserve/ItemCrayon/ItemAppleSchrabidium/ItemTemFlakes), and ItemEnergy/ItemPill/ItemCanteen are missing. `items/gear` has 6 of the ~13 planned files. Needs substantial finishing work - read `docs/phase1/items_food_gear.md` and complete it. |
| 10 | `items_special` | `com.hbm.items.special.*` incl. `BedrockOre*`/`ItemBedrockOre*` | 38 files present, including the dedicated `ItemBedrockOreNew`/`ItemBedrockOreBase` sub-area (156-variant flattening) which DID land. Coverage of the rest of the ~19+flatten "P1 register now" list from `items_special.md` needs verification - likely partial. |
| 11 | `items_machine` | `com.hbm.items.machine.*` (target 43 files) | 48 files present - meets or exceeds the target count, but verify against `items_machine.md`'s exact list rather than trusting the count alone (could include duplicates/wrong scope). |
| 12 | `blocks_generic_*` (3 sub-areas merged into one package) | `com.hbm.blocks.generic.*` (target ~95 files across 3 research slices) | 77 files present - a solid majority but likely missing pieces from one or more of the 3 slices (structural/doors/glass; ore/plants/fallout; hazard-adjacent/crates/deco/misc). Cross-check against `blocks_generic.md`'s full 95-file list. |
| 13 | *(merged into #12 at planning time)* | `com.hbm.blocks.gas.*` (3 safe files: BlockGasFlammable/Explosive/Base) | **Not verified present or absent - check `com.hbm.blocks.gas` explicitly, it may have been missed entirely.** |
| 14 | *(no separate area - hazard/tab wiring was distributed into every content area)* | `HazardRegistry.registerItems()` body, `CreativeTabContents.add(...)` calls | Each area was instructed to add its own hazard/tab wiring as it went. Since most areas skipped review, **assume hazard/tab wiring is incomplete or inconsistent until spot-checked.** |
| 15 | *(same as14)* | - | - |

(The table has 15 numbered rows in the original plan; #13/#14/#15 collapsed as noted above since
those responsibilities were folded into other areas rather than being standalone agent slots.)

**Shared files that got additive edits from multiple areas** (check these compile and make
sense as a whole, since several different agents appended to them independently):
`src/main/java/com/hbm/items/ModItems.java`, `src/main/java/com/hbm/blocks/ModBlocks.java`,
`src/main/java/com/hbm/hazard/HazardRegistry.java`, `src/main/java/com/hbm/creativetabs/ModCreativeTabs.java`.

## What to do next (in order, use your own judgment on pacing/batching)

1. **Verify, don't blindly trust.** For every Phase 1 area above marked anything other than
   "trustworthy", read its `docs/phase1/<area>.md` research report, compare against what
   actually exists on disk, and finish/fix what's missing or wrong. Where a review never ran,
   consider running one (an adversarial re-read against the CE source) before extending it
   further, so you're not building on a broken foundation.
2. **Finish Phase 1** to the same standard Phase 0 hit: every area implemented, reviewed, fixed;
   `gradlew compileJava` error count triaged and every remaining error explained by a documented
   later-phase forward reference (update `docs/phase1/STATUS.md` - create it, mirroring
   `docs/phase0/STATUS.md`'s structure - once you've done this triage).
3. **Continue through Phases 2-6** per `PORT_SPEC.md`, in order, using the same methodology
   established in Phase 0/1: for each phase, run a research wave (agents mapping the relevant CE
   packages, producing `docs/phase<N>/*.md` reports), digest the findings into concrete work
   packages (aim for the spec's mandated 15 per wave, rebalance package boundaries as needed,
   same as this session did), then implement -> review -> fix. Use the `Workflow` tool for this
   orchestration (pipeline research->implement->review->fix per area, same pattern as the
   existing `docs/phase0/DIGEST.md` and `docs/phase1/*.md` work was produced with) if your
   environment has it; otherwise coordinate manually but keep the same quality bar (independent
   review of every area against CE, no invented APIs, no placeholder content for what you do
   implement).
4. **Commit and push frequently.** Use real, descriptive commit messages (see the existing git
   log for tone/style: `git log --oneline`). Don't wait until a whole phase is done - commit
   after each area or logical chunk lands, so progress is never lost to an interruption again
   (this exact handoff exists because a previous run got killed mid-wave without committing).
5. **Do not stop to ask the user questions.** They are unreachable. Where the research reports
   flag an open design decision (there are several - Baubles vs Curios for detector items,
   exact tier-naming for a couple of food items, whether `blocks/rail` is Phase 2 or Phase 4,
   etc.), make a reasoned engineering call yourself, document it clearly in the relevant
   `docs/phase<N>/STATUS.md` or a new decisions log, and proceed. Prefer the choice that best
   matches CE's actual behavior and the project's stated 99%-parity goal.
6. **`gradlew build` will not be fully green until well into this work** (see
   `docs/phase0/STATUS.md`'s "On 'gradlew build green' as a per-phase gate" section) - that is
   expected for a strictly top-down phased port of a ~4000-file monolith, not a sign of failure.
   Track error-count trend and triage explanations instead of chasing a premature green build.

## Key design decisions already made (do not relitigate without strong reason)

- Modid stays `hbm`, package `com.hbm`, preserving CE's registry ids wherever legal in 1.21.
- Custom HE energy system stays a distinct capability, not collapsed into NeoForge Energy.
- Material-shape items (ingot/plate/wire/etc as a system) use `MaterialShapes.buildRegistryName`
  producing suffix-first ids (`iron_ingot`, `titanium_block`) - a deliberate departure from CE's
  prefix-first ids (`ingot_iron`, `block_titanium`), applied consistently to items AND blocks.
- Metadata-flattening: every CE item/block that used damage-value metadata to encode variants
  becomes N distinct registry entries. Large *irregular* hand-coded material-named families
  (ingot_/nugget_/billet_/powder_/plate_/crystal_/waste_/fragment_) are NOT generalized into a
  single generic loop (CE itself never did, and the material sets are inconsistent across them) -
  they stay individually hardcoded `DeferredItem` fields, optionally behind a thin
  `registerX(name)` helper for the plain-behavior majority.
- Fluid-backed items (tanks, cells, cassettes with open-ended track registries) are NOT flattened
  per-fluid/per-variant - one item + a Data Component holding the identity, since those
  registries are open-ended.
- `CreativeTabContents.add(tab, supplier)` is the shared mechanism for tab population - content
  areas call it themselves, never edit `ModCreativeTabs.java` directly.
- `HazardSystem.register(...)` (7 overloads) is the shared mechanism for hazard bindings -
  content areas call it themselves for their own hazardous items/blocks.
- Datagen providers iterate whatever is actually registered at datagen time (never hardcode an
  expected item/block list) and re-derive common-material tags directly from
  `Mats`/`MaterialShapes` rather than requiring cross-area coordination.

## Where things live

- `PORT_SPEC.md` - the full 7-phase specification, read it in full.
- `docs/phase0/` - Phase 0's research/implementation record (DIGEST.md, STATUS.md).
- `docs/phase1/` - Phase 1's research reports (12 files) and this session's synthesis notes.
- `upstream/` - gitignored, re-clone per the setup section above.
- Standard NeoForge/Gradle layout otherwise (`src/main/java/com/hbm/...`,
  `src/main/resources/...`, `build.gradle`, `gradle.properties`).
