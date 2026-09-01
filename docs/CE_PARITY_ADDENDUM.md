# Agent addendum: 1-to-1 CE parity (non-negotiable)

Read this **after** `PORT_SPEC.md` and **before** writing any code.

This file does not replace the spec. It locks the quality bar the owner actually wants: a port that plays like Community Edition, not a census score that looks finished.

Russian one-liner for the owner: каждая зарегистрированная вещь должна работать как в CE — логика, GUI, рецепты, текстуры, звуки. Weighted 100% ≠ готово.

## Goal

Ship a **1-to-1 functional port** of HBM CE `v2.5.0.5` to Minecraft 1.21.1 / NeoForge 21.1.x.

- Same items, blocks, fluids, entities, machines, weapons, reactors, worldgen, radiation, pollution, fallout, guns, missiles, GUIs.
- Same numbers, recipe graphs, and player-facing behavior.
- Same look: CE textures, sounds, lang. No purple-black missing textures on anything that exists in the registry.
- `modId` stays `hbm`. Registry ids stay CE ids wherever 1.21 allows.

If CE has it and it is not on the tiny exclusion list below, the port must have it **and it must work**.

## Source of truth

| Source | Use for |
|---|---|
| `upstream/hbm-ce` (Warfactory CE) | Content, numbers, behavior, assets, recipes |
| `upstream/neo-edition` | NeoForge 1.21.1 API shape only |
| Weighted census / `docs/phase11/PARITY_REPORT.md` | Progress dashboard, **not** done |

Never copy Neo Edition simplifications over CE behavior.
Never invent machines, recipes, items, or textures CE does not have.
Never skip a CE feature because the census already says ≥99%.

## Rule 1 — registered means functional

If a block, item, fluid, entity, menu, or machine is in the registry, it is not a decoration.

A registered machine must:

1. Place and form its multiblock the CE way (`BlockDummyable` dims).
2. Have a live block entity, not an empty casing.
3. Open the real menu + screen (`SafeMenuScreens.bind` is a crash guard, not a substitute for the GUI).
4. Consume and produce from the **real CE recipe table**, ported as JSON / serializer data, not a stub list.
5. Move energy / fluid / items through the same sides and caps CE uses.
6. Tick the extra CE systems that machine actually touches (pollution, heat, radiation, upgrades) — or leave a `TODO(CE: file:line)` naming the exact skipped call.

A registered block that is only a cube with no TE, when CE has a TE, is a defect. Do not call it "ported".

## Rule 2 — assets ride with the object

Every registered object ships with CE texture (copied, not redrawn), blockstate/model, en_us lang (and ru_ru when CE has it), sound if CE plays one, recipe/loot if CE has one.
Missing-texture on a live registry id is a blocker, same as a compile error.
Do not invent art. If CE has no file, document it in `docs/phase10/LEFTOVER_MISSES.md` and keep the id unbound to fake art.

## Rule 3 — no silent drops

Forbidden: commenting out CE calls; PASS/empty output to compile; generic chest GUI instead of CE overlay; empty recipe table when CE has I/O; skipping pollution/particles/upgrades/radiation without a cited TODO; treating weighted ≥99% or green runServer as feature-complete.
Deferred behavior must be `// TODO(CE: com.hbm...File.java:NN): <what CE does> — blocked by <missing type>. Do not invent.`

## Rule 4 — recipe graph must close

Every CE craft whose inputs and outputs exist must exist in the port.
Every machine family CE registers (ChemPlant, SILEX, StorageDrum, SuperComputer, assembler leftovers, anvil, crucible, press, …) keeps its table.
Do not freeze machine recipes at 83.7% and grind Dummyable cubes instead.
Do not emit JSON that references an unregistered id. Register the missing I/O, then the row.
Banned / commented / LBSM-gated / 1.12-only integration rows stay on the exclusion list with a CE cite. Everything else ships.
Current ~53% reachability is not acceptable as a final number.

## Rule 5 — play it like CE, not like a checklist

Before closing a package: can a player obtain the item the CE way? Does the block do the CE job? Does the GUI show CE info? Do hazards still apply? Do sounds/textures match? Would a CE player recognize the machine?
If the honest answer is "it registers and the server boots", the package is not done.

## Allowed exclusions (tiny)

Only: dead/deprecated CE; 1.12-only integrations; things 1.21 cannot represent; CE's own unfinished bits (Soyuz LAUNCHING TBI in CE); CE `processor/` ARR. That list is ≤1% of CE. ChemPlant, SILEX, StorageDrum, SuperComputer, missing Dummyables, missing vanilla crafts, missing textures — NOT exclusions.

## What is not done today

Machine recipes still ~83.7% (ChemPlant/SILEX/StorageDrum/SuperComputer empty because I/O never registered). Blocks short. Reachability ~53%. Texture misses. Skipped per-machine hooks. Trust STATUS.md + this file + PORT_SPEC, not stale HANDOFF.
Work the holes, do not grow the census with extra flatten ids.

## Definition of done

PORT_SPEC §5 plus: every registered object works like CE; real CE assets; survival path ore→steel→assembler→chemplant→RBMK→nuke without creative; exclusion list short and cited; weighted ≥99% is a gate not the goal. The goal is plays like CE.
