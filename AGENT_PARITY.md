# Agent addendum: 1-to-1 CE parity (non-negotiable)

Read this **after** `PORT_SPEC.md` and **before** writing any code.
This file does not replace the spec. It locks the quality bar the owner
actually wants: a port that plays like Community Edition, not a census
score that looks finished.

Russian one-liner for the owner: каждая зарегистрированная вещь
должна работать как в CE — логика, GUI, рецепты, текстуры, звуки.
Weighted 100% ≠ готово.

## Goal

Ship a **1-to-1 functional port** of HBM CE `v2.5.0.5` to
Minecraft 1.21.1 / NeoForge 21.1.x.

- Same items, blocks, fluids, entities, machines, weapons, reactors,
  worldgen, radiation, pollution, fallout, guns, missiles, GUIs.
- Same numbers, recipe graphs, and player-facing behavior.
- Same look: CE textures, sounds, lang. No purple-black missing
  textures on anything that exists in the registry.
- `modId` stays `hbm`. Registry ids stay CE ids wherever 1.21 allows.

If CE has it and it is not on the tiny exclusion list below, the port
must have it **and it must work**.

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

If a block, item, fluid, entity, menu, or machine is in the registry,
it is not a decoration.

A registered machine must:

1. Place and form its multiblock the CE way (`BlockDummyable` dims).
2. Have a live block entity, not an empty casing.
3. Open the real menu + screen (`SafeMenuScreens.bind` is a crash
   guard, not a substitute for the GUI).
4. Consume and produce from the **real CE recipe table**, ported as
   JSON / serializer data, not a stub list.
5. Move energy / fluid / items through the same sides and caps CE uses.
6. Tick the extra CE systems that machine actually touches
   (pollution, heat, radiation, upgrades) — or leave a
   `TODO(CE: file:line)` naming the exact skipped call.

A registered block that is only a cube with no TE, when CE has a TE,
is a defect. Do not call it "ported".

## Rule 2 — assets ride with the object

Every registered object ships with:

- CE texture (copied, not redrawn, not a placeholder)
- blockstate / model (datagen or CE-equivalent)
- `en_us` lang entry; copy `ru_ru` and other CE langs when present
- sound event wired if CE plays one
- recipe and/or loot path if CE has one

Missing-texture / missing-model / missing-lang on a live registry id
is a **blocker**, same as a compile error.

Do not invent art. If CE has no file, document it in
`docs/phase10/LEFTOVER_MISSES.md` and keep the id unbound to fake art.

## Rule 3 — no silent drops

Forbidden:

- Commenting out a CE call and moving on
- Returning `InteractionResult.PASS` / empty output to "compile"
- Replacing a CE overlay / special GUI with a generic chest menu
  and calling it done
- Registering the block but leaving the recipe table empty
  ("I/O unregistered, not invented") when CE has the I/O
- Skipping pollution, particles, animation, canister load/unload,
  upgrade slots, or radiation hooks without a cited TODO
- Treating "weighted ≥99%" or a green `runServer` as feature-complete

If you must defer a CE behavior:

```
// TODO(CE: com.hbm.inventory.recipes.ChemplantRecipes.java:NN):
//   <what CE does> — blocked by <missing type>. Do not invent.
```

The TODO must name the CE file and line. A bare `// TODO later` is a
silent drop.

## Rule 4 — recipe graph must close

PORT_SPEC DoD includes reachability. Current ~53% is **not acceptable
as a final number**.

- Every CE craft whose inputs and outputs exist must exist in the port.
- Every machine family CE registers (ChemPlant, SILEX, StorageDrum,
  SuperComputer, assembler leftovers, anvil, crucible, press, …)
  keeps its table. Do not freeze machine recipes at 83.7% and grind
  Dummyable cubes instead.
- Do not emit a JSON recipe that references an unregistered id
  (that breaks `runServer`). Register the missing I/O, then the row.
- Banned / commented / LBSM-gated / 1.12-only integration rows stay
  on the exclusion list with a CE cite. Everything else ships.

## Rule 5 — play it like CE, not like a checklist

Before closing a package, the agent must be able to answer "yes":

- Can a player obtain the item the CE way (craft, machine, worldgen, loot)?
- Does the block do the CE job when used, not only sit in a tab?
- Does the GUI show CE information (energy, tanks, progress, slots)?
- Do hazards, radiation, and pollution still apply?
- Do sounds and textures match CE?
- Would a CE player recognize the machine without reading our STATUS.md?

If the honest answer is "it registers and the server boots", the
package is not done.

## Allowed exclusions (tiny)

Only these classes may stay out, and each needs an owner-visible cite:

- Dead / deprecated CE content
- 1.12-only mod integrations (GregTech, Baubles-as-Baubles, Mekanism
  comments already disabled in CE)
- Things 1.21 physically cannot represent (double-slab block)
- CE's own unfinished bits (Soyuz `LAUNCHING` is TBI **in CE**)
- CE `processor/` (All Rights Reserved — do not copy)

That list is **≤1%** of CE. ChemPlant, SILEX, StorageDrum recipes,
SuperComputer drives, missing Dummyables, missing vanilla crafts,
missing textures on registered ids — **not** exclusions.

## What is not "done" today

Do not treat these as accepted leftovers when you pick up the tree:

- Machine recipes still ~83.7% — ChemPlant / SILEX / StorageDrum /
  SuperComputer tables empty because I/O was never registered
- Blocks still short of CE (Dummyable / deco leftover)
- Item reachability ~53%
- Texture misses on items/blocks that CE already ships files for
- Skipped per-machine hooks: pollution ticks, canister `loadTank`,
  `UpgradeManagerNT`, particles, shred entities, RBMK waste heat path
- `HANDOFF.md` / `docs/HANDOFF_NEXT_AGENT.md` may be stale — trust
  `docs/phase11/STATUS.md` + this file + `PORT_SPEC.md`

Work the holes, do not grow the census with extra flatten ids.

## Definition of done (owner bar)

`PORT_SPEC.md` §5 still applies. This addendum adds:

1. Every registered object works like CE (logic + GUI + I/O).
2. Every registered object has real CE assets (texture / model / lang / sound).
3. Recipe graph reachability is high enough that a survival player can
   walk ore → steel → assembler → chemplant → RBMK → nuke without
   creative.
4. Exclusion list is short, cited, and approved — not "whatever we
   skipped this wave".
5. Weighted census ≥99% is a **gate**, not the goal. The goal is
   "plays like CE".

## Read order for a new agent

1. `PORT_SPEC.md`
2. **this file**
3. `docs/phase11/STATUS.md` + `docs/phase11/PARITY_REPORT.md`
4. The research doc for the package you are about to touch
5. The CE source file. Then write code.
