# Build verification handoff — testing agent instructions

**Audience: an agent (or human) with real, unrestricted network access — NOT the sandbox that wrote
this port.** That sandbox has never once been able to reach `maven.neoforged.net` or
`maven.blamejared.com` (confirmed HTTP 403 from its egress proxy, an organization network policy,
not a fixable client issue) — so **no code in this repository has ever been compiled**, from Phase 0
through Phase 6. This document exists to get that first real signal.

## Your job, precisely

**Build this project, run its data generation, capture every error, and report them. Do not fix
anything.** No edits, no commits, no "while I was in there I also changed X." Your entire output is
one markdown report. A separate pass will do the fixing, informed by your report — mixing the two
roles here would make the report a moving target and hide the true error count/order.

If you are an AI agent: treat "do not fix" as a hard constraint even when a fix looks trivial or
obvious. Report it instead. This is deliberate — the fixing pass needs the *complete, unfiltered*
error picture in one shot, not a partially-pre-cleaned one.

## Prerequisites

- JDK 21 (the project already pins `org.gradle.java.home` in `gradle.properties` to
  `/usr/lib/jvm/java-21-openjdk-amd64` — if that path doesn't exist in your environment, either
  install a JDK 21 there or edit that one line locally, but **do not commit that edit**).
- Real outbound network access to `maven.neoforged.net`, `maven.blamejared.com`, and the usual
  Maven Central / Gradle Plugin Portal hosts. If you're behind a proxy that also blocks these, this
  handoff won't get further than the sandbox already did — report that plainly rather than
  guessing around it.
- The repository already has `gradlew`/`gradlew.bat` and a checked-in Gradle wrapper — no local
  Gradle install needed.

## Steps, in order — stop and report as soon as a step fails, don't skip ahead

Run each from the repo root. Redirect full output to a log file for each (`| tee stepN.log` or
similar) — you'll need the raw text for the report, not just the pass/fail.

1. **`./gradlew --version`** — sanity check the wrapper itself launches and confirm the JDK it
   picked up. Include this output verbatim at the top of your report (environment fingerprint).

2. **`./gradlew compileJava`** — the fastest, most important signal. This is a from-scratch build
   in a network-connected environment, so expect it to first resolve and download the NeoForge
   21.1.228 toolchain (NeoForm decompile, patches, mappings) — that alone can take several minutes
   and produce a lot of download-progress noise; that's normal, not an error. The actual compiler
   output (errors, if any) comes after that. **This step alone is the single most valuable thing
   you can report** — every phase of this port was written without ever seeing a real `javac`
   diagnostic.

3. **If step 2 succeeds:** `./gradlew build` (skip tests if the project has none configured, that's
   expected — this project has no JUnit test suite). This exercises `processResources`,
   `neoForgeSourceSets`/mod-jar assembly, etc. beyond pure `.java` compilation.

4. **If step 2 or 3 succeeds:** `./gradlew runData`. This runs the mod's own `GatherDataEvent`
   pipeline (see `build.gradle`'s `runs { data { ... } }` block) — generates every item/block model,
   blockstate, loot table, and lang entry via the exhaustive-`DeferredRegister`-iteration providers
   this project's own docs describe (`docs/phase5/advancement_and_recipe_datagen_assets.md` §2.2).
   This is PORT_SPEC.md's Definition-of-done criterion #4 ("no unregistered assets, datagen
   verify") — a clean, crash-free `runData` run is the actual test for that criterion. Report
   whether it completed, and if not, the exact exception/stack trace. **You do not need to inspect
   the generated JSON output by hand** — just whether the generator itself ran to completion.

5. **Optional, only if you have time/capacity and steps 1-4 all passed:** `./gradlew runClient`
   (or `./gradlew runServer` if a headless boot is easier for you) just far enough to see whether the
   game boots to the title screen / dedicated server reaches "Done" without an immediate mod-loading
   crash. **This is a boot-smoke-test only, not gameplay** — do not attempt to actually play or
   test features; `docs/phase6/playtest_scenarios.md` (already written, in this same repo) is the
   script for that, and it's a separate, later task once compilation is confirmed green. If the
   client/server needs a display or takes too long to be practical for you, skip this step and say
   so — it's the lowest-priority of the five.

## What to put in the report

One file: `docs/phase6/BUILD_ERRORS.md` (or hand it back however you were asked to — a single
markdown file either way). Structure:

1. **Environment fingerprint**: OS, `./gradlew --version` output, confirmation that
   `maven.neoforged.net`/`maven.blamejared.com` were actually reachable (not just "assumed
   reachable").
2. **Per-step result table**: step name, pass/fail/skipped, wall-clock time if easy to note.
3. **Full raw compiler output for every failing step**, not a paraphrase — copy the actual `javac`/
   Gradle error blocks verbatim (file path, line number, the exact diagnostic message). If the
   output is extremely long, it's fine to truncate repeated boilerplate (e.g. a thousand identical
   download-progress lines) but never truncate an actual `error:` line.
4. **A best-effort grouping**, after the raw dump: many real compile errors cascade from one root
   cause (a missing import breaks every file that transitively depends on the class using it, one
   wrong method signature can show up at every call site). If you can tell that 40 errors all trace
   back to one bad symbol, say so explicitly and group them — this saves the fixing pass from
   re-discovering that connection independently. When you're not sure two errors are related, list
   them separately rather than guessing at a grouping.
5. **Explicit "did not attempt to fix" statement** at the end, plus anything you noticed but didn't
   have a clean way to test (e.g. "network access was fine for Maven Central but I couldn't tell if
   X mirror is geographically distant enough to matter").

## Context that may help you interpret what you find (informational only — again, don't act on it)

- This project ported `com.hbm.*` 1:1 from a 1.12.2 Forge mod to 1.21.1 NeoForge across 6 phases,
  entirely via static reading of the original source (no compiler feedback loop at any point).
  `docs/phase0/STATUS.md` through `docs/phase6/STATUS.md` are the full paper trail if a specific
  error's context would help — grep them for the class name involved.
- Known, already-disclosed risk areas most likely to surface first (from `docs/phase6/STATUS.md`
  §2's own words): "a wrong-arity method resolving to an unexpected overload, a generics mismatch,
  an API shape that drifted from what `upstream/neo-edition` showed." Two *specific* bug patterns
  recur throughout this codebase's own history and are worth recognizing if you see them in the
  error output (though you still shouldn't fix them, just note if you spot the pattern): (a) a
  `DeferredHolder`/registry-holder `.get()` call inside a `static` field initializer — this throws
  `IllegalStateException` at class-load time, not a compile error, so it would show up as a runtime
  crash during step 5, not steps 2-4; (b) a class whose `@EventBusSubscriber` needs
  `bus = EventBusSubscriber.Bus.MOD` but is missing it — also a runtime symptom (a handler silently
  never firing), not a compile error.
- `upstream/neo-edition` (checked out in this repo) is a separate, much smaller, real, *compiling*
  NeoForge 1.21.1 mod pinned to the exact same `neo_version=21.1.228` — if you want a quick sanity
  check that your own environment/toolchain is set up correctly before trusting a failure as *this*
  port's fault, you could try `cd upstream/neo-edition && ./gradlew compileJava` first (it's a
  separate Gradle project with its own wrapper) — if that also fails the same way, the problem is
  your environment, not this port's code.

## When you're done

Hand back `docs/phase6/BUILD_ERRORS.md` (or wherever you were told to put it) with nothing else
modified. If everything passed clean — genuinely possible, six phases of disciplined,
CE-cross-referenced work went into this — say that plainly too; a report that says "0 errors, here's
proof" is just as valuable as one full of them.
