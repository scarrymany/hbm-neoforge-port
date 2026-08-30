# Implementation notes: `IngotNuggetItems` (`ingot_`/`nugget_` resource items)

Design-decision log for `com.hbm.items.IngotNuggetItems`, the port of CE's ~174 hardcoded
`ingot_`/`nugget_` fields (`ModItems.java` ~L780-1137). This is not a research report - it's the
per-file record that class's javadoc points at, covering decisions made both at initial port time
and in a later correction pass.

## Metadata flattening

- `ingot_steel_dusted` (CE: `ItemHotDusted`, damage 0-9 = forging purity level) becomes
  `ingot_steel_dusted_0` .. `ingot_steel_dusted_9`. Confirmed still-live via `AnvilRecipes`'s
  purity-combining smithing recipes and `SmeltingRecipes`.
- `ingot_u238m2` (CE: `ItemUnstable`, damage 1-3 = inert "ELEMENTS"/"ARSENIC"/"VAULT" reskins)
  becomes `ingot_u238m2`, `ingot_u238m2_elements`, `ingot_u238m2_arsenic`, `ingot_u238m2_vault`.
  `nugget_u238m2` shares the same CE class but has zero references to those metas anywhere in CE's
  codebase, so it stays a single item rather than inventing three unused variants.

## Deferred behavior

- **`ItemUnstable`'s nuclear-detonation timer** (`ingot_u238m2*`, `nugget_u238m2`,
  `ingot_electronium`) depends on CE's `EntityNukeExplosionMK5`/`EntityNukeTorex`/`ModDamageSource`,
  a nuke-explosion engine that belongs to a dedicated later phase, not "items and simple blocks".
  These fields register as plain `Item`.
- **`ItemHotDusted`'s per-purity-level heat mechanic** (`ingot_steel_dusted_0..9`) is deferred: no
  `ItemHotDusted` port exists yet. Porting it would mean either 10 near-duplicate heat-tracking
  classes (one per purity level's data) or a shared generalization of `ItemHot` itself to carry a
  purity index, and that class lives in `com.hbm.items.special`, owned by a different area. These
  fields register as plain `Item` pending that class.
- **`ItemCustomLore`'s "polaroid ID 11" easter-egg branch** (the `.desc.P11` lines CE showed only
  when `MainRegistry.polaroidID == 11`, on `ingot_lanthanium`, `ingot_neptunium`, `ingot_tantalium`,
  `nugget_tantalium`) is not reproduced - it depends on an unported mechanic
  (`MainRegistry.polaroidID`). The port's `com.hbm.items.special.ItemCustomLore` only ever renders
  the plain `.desc` line (CE's own fallback whenever `polaroidID != 11`), so these four items already
  show their non-Easter-egg CE tooltip faithfully; only the rare P11 flavor swap is missing.

## Flavor-text tooltip fix (2026-08-30 correction pass)

12 fields have a real `item.hbm.<name>.desc` lang entry in CE's `en_us.lang`
(`ingot_asbestos`, `ingot_combine_steel`, `ingot_euphemium`, `ingot_fiberglass`, `ingot_gh336`,
`ingot_lanthanium`, `ingot_neptunium`, `ingot_tantalium`, `nugget_euphemium`, `nugget_gh336`,
`nugget_mox_fuel`, `nugget_tantalium`) but were registered as plain `Item`, silently dropping that
tooltip. CE backed these with `ItemCustomLore`/`ItemBakedBase`. Rather than writing a new tooltip
helper, this file now constructs the port's existing `com.hbm.items.special.ItemCustomLore` for
these 12 fields (via two small `registerLoreIngot`/`registerLoreNugget` wrappers next to
`registerIngot`/`registerNugget`) - that class already exists in this port (added by the
`items_special` area) and already reproduces exactly CE's non-P11 `.desc` lookup-and-`$`-split
behavior; `com.hbm.items.special.SpecialItems` already constructs it the same way for
`demon_core_closed`. Writing a second, duplicate tooltip mechanism in this file would have
diverged from that precedent for no benefit. `ingot_gh336` additionally keeps its `Rarity.EPIC`
property, now passed into `ItemCustomLore` instead of a plain `Item`.

## Heat-glow fix (2026-08-30 correction pass)

`ingot_chainsteel`, `ingot_meteorite`, `ingot_meteorite_forged` are backed by `ItemHot` in CE but
were registered as plain `Item`, losing the heat-countdown behavior entirely (`com.hbm.items.
special.ItemHot` already exists in this port but nothing constructed it). This was blocked at
initial port time because `ItemHot` needs a `DataComponentType<Integer>` registered on the mod
event bus; that component (`SpecialItemComponents.HEAT`) has since been added and wired in
(`ModItems.register()` calls `SpecialItemComponents.register(modEventBus)`), so the blocker is
gone. These three fields now construct `new ItemHot(properties, maxHeat)` directly, with `maxHeat`
taken from CE's `ModItems.java` lines 884-886 (`ingot_chainsteel` = 100, `ingot_meteorite` = 200,
`ingot_meteorite_forged` = 200). `ItemHotDusted`'s parallel mechanic remains deferred (see above) -
that class doesn't exist yet, unlike `ItemHot`.
