# Phase 1 research: ModItems.java generative / material-driven resource items

Scope: `com.hbm.items.ModItems` in hbm-ce (2855 lines, ~1800 `public static final Item` fields
total). This report covers only the item families that are either (a) genuinely generated from
the `Mats`/`MaterialShapes` system via `ItemAutogen`, (b) genuinely generated from a Java `enum`
via `ItemEnumMulti` (metadata-driven-multi, needs post-flattening expansion), or (c) hand-written
per-material fields that are clearly "the same shape of item, repeated per material" and are
candidates for folding into a data-driven registration loop even though CE itself did not do so.
Everything else in the file (tools, armor, weapons/missiles, food, batteries, upgrades, etc.) is
listed briefly at the end as explicitly out of scope for this area.

## Headline finding: `NTMMaterial.autogen` is not 1:1 with "this shape has an ItemAutogen item"

This is the single most important thing for whoever implements this area to know before writing
a generation loop keyed off `mat.autogen.contains(shape)`.

`Mats.java` assigns `setAutogen(...)` shapes to materials for **two unrelated consumers**:

1. `ItemAutogen` (`com.hbm.items.special.ItemAutogen`) - iterates `Mats.orderedList`, and for a
   shape it owns, generates one metadata variant per material where `mat.autogen.contains(shape)`.
   Only **17 of the ~25 shape constants are ever actually passed to `new ItemAutogen(...)`** in
   ModItems.java (see table below).
2. `OreDictManager` / `MatDistribution` - the *same* `mat.autogen` set also drives ore-dictionary
   prefix registration (`OreDictManager.java:640`, `.addPrefix(HEAVY_COMPONENT, true)` at line 782)
   and crucible material-composition lookups, **independently of whether an `ItemAutogen` item
   exists for that shape**. For shapes with no live `ItemAutogen` call (NUGGET, DUST, DUSTTINY,
   BILLET, PLATE, GEM, BLOCK), CE instead hand-registers one `Item` field per material under a
   *different* naming convention (`nugget_x`, `powder_x` / `powder_x_tiny`, `billet_x`, `plate_x`,
   `gem_x`/`crystal_x`, and BLOCK backs a real `Block` in ModBlocks, not an `Item`), and wires each
   one back into the material system individually via `MatDistribution.registerEntry(...)`
   (a different file/area, not itemization logic).

Consequence for the port: do not build "one generic per-shape generation loop driven by
`mat.getAutogen()`" and expect it to cover ingots/nuggets/dusts/billets/plates/gems - it only
correctly covers the 17 shapes in the table below. The rest need the hand-enumerated approach
(section 3).

## 1. True `ItemAutogen`-backed families (shape -> compatible materials via `mat.autogen.contains(shape)`)

Verified via `com.hbm.items.special.ItemAutogen` (constructor stores `MaterialShapes shape`;
`getSubItems`/`registerModels`/`ownsModelLocation` all gate on `mat.autogen.contains(this.shape)`)
cross-referenced against every `setAutogen(...)` call in CE's `Mats.java` (already ported
byte-for-byte in Phase 0's `Mats.java`/`NTMMaterial.java`/`MaterialShapes.java`, using
`MaterialShapes.shapeByRegistryName` names). There are exactly **17** `new ItemAutogen(...)`
declarations in ModItems.java, all clustered at lines 1246-1272, totaling **188** generated
(material, shape) item variants.

| CE field | MaterialShapes | Compatible materials (CE `MAT_*`, lowercased) | count |
|---|---|---|---|
| `shell` | SHELL | titanium, copper, aluminium, steel, weaponsteel, saturn | 6 |
| `pipe` | PIPE | iron, copper, aluminium, lead, steel, dura, rubber | 7 |
| `ingot_raw` | INGOT | redstone, neodymium, borax, sodium, strontium, slag | 6 |
| `plate_cast` | CASTPLATE | iron, gold, schrabidium, schrabidate, titanium, copper, tungsten, aluminium, lead, zirconium, osmiridium, steel, dura, desh, star, ferro, tcalloy, cdalloy, bbronze, abronze, cmb, weaponsteel, saturn | 23 |
| `plate_welded` | WELDEDPLATE | iron, titanium, copper, tungsten, aluminium, zirconium, osmiridium, steel, tcalloy, cdalloy, cmb | 11 |
| `heavy_component` | HEAVY_COMPONENT | **none** - no material currently has `.setAutogen(HEAVY_COMPONENT)` | 0 |
| `wire_fine` | WIRE | carbon, gold, schrabidium, copper, tungsten, aluminium, lead, zirconium, steel, mingrade, magtung | 11 |
| `wire_dense` | DENSEWIRE | gold, schrabidium, schrabidate, titanium, copper, tungsten, neodymium, niobium, mingrade, star, bscco, magtung, dnt | 13 |
| `bolt` | BOLT | tungsten, lead, steel, dura | 4 |
| `part_barrel_light` | LIGHTBARREL | steel, dura, desh, tcalloy, cdalloy, bbronze, abronze, gunmetal, weaponsteel, saturn | 10 |
| `part_barrel_heavy` | HEAVYBARREL | steel, dura, desh, ferro, tcalloy, cdalloy, gunmetal, weaponsteel, saturn | 9 |
| `part_receiver_light` | LIGHTRECEIVER | steel, dura, desh, tcalloy, cdalloy, bbronze, abronze, gunmetal, weaponsteel, saturn | 10 |
| `part_receiver_heavy` | HEAVYRECEIVER | dura, ferro, tcalloy, cdalloy, bbronze, abronze, gunmetal, weaponsteel, saturn | 9 |
| `part_mechanism` | MECHANISM | gunmetal, weaponsteel, saturn | 3 |
| `part_stock` | STOCK | wood, desh, gunmetal, weaponsteel, saturn, polymer, bakelite, hardplastic, pvc | 9 |
| `part_grip` | GRIP | wood, ivory, steel, dura, desh, gunmetal, weaponsteel, saturn, polymer, bakelite, rubber, hardplastic, pvc | 13 |
| `bedrock_ore_fragment` | FRAGMENT | coal, lignite, diamond, iron, gold, redstone, bauxite, cryolite, uranium, u238, thorium, polonium, technetium, radium, titanium, copper, tungsten, aluminium, lead, bismuth, tantalium, neodymium, niobium, beryllium, emerald, cobalt, boron, borax, lanthanium, zirconium, sodium, sodalite, strontium, lithium, sulfur, kno, fluorite, phosphorus, chlorocalcite, molysite, cinnabar, silicon, asbestos, rareearth | 44 |

Notes on individual fields:

- **`heavy_component` generates zero variants today.** It is fully wired end-to-end (a real
  `MaterialShapes` constant, `OreDictManager` prefix registration, an `ANY_RESISTANTALLOY` ore-dict
  group reference), but no `NTMMaterial` currently calls `.setAutogen(HEAVY_COMPONENT)`. Register
  the `Item` for structural parity (mods/datapacks could still reference `hbm:heavy_component`) but
  expect it to produce nothing by default - flag this to whoever owns Mats.java in case it is a
  regression rather than intentional.
- **`plate_cast` / `bedrock_ore_fragment`** each carry a `.aot(Mats.MAT_BISMUTH, "...")` texture
  override even though `MAT_BISMUTH` is not in either shape's autogen set (BISMUTH has FRAGMENT,
  not CASTPLATE) - dead/vestigial override, safe to drop, but worth a one-line comment in the port
  noting it was intentionally not carried over from CE. `bedrock_ore_fragment`'s bismuth override
  path is likewise not reachable since bismuth *is* in FRAGMENT's set - re-check at implement time;
  either way it is a texture-path detail, not a registration-shape decision.
- **`wire_fine`** carries 8 explicit `.aot(mat, texture)` overrides (aluminium, copper, mingrade,
  gold, tungsten, carbon, schrabidium, magtung) out of its 11 compatible materials; lead, zirconium,
  steel fall back to the default `wire_fine-<matname>` texture path. Texture-path detail only, does
  not change the registry-id generation rule.
- **`bolt`** calls `.oun("boltntm")`, overriding its translation key namespace - a display-name
  detail, not a registry-id concern (registry ids still come from `buildRegistryName`).

### Recommended port strategy for this family

**Data-driven registration loop, one per shape**, keyed off `NTMMaterial.autogen.contains(shape)`
exactly like CE's `ItemAutogen` does - this is precisely what Phase 0's
`MaterialShapes.buildRegistryName(NTMMaterial)` was built for. Concretely:

```java
for (MaterialShapes shape : List.of(SHELL, PIPE, INGOT, CASTPLATE, WELDEDPLATE, HEAVY_COMPONENT,
        WIRE, DENSEWIRE, BOLT, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER,
        MECHANISM, STOCK, GRIP, FRAGMENT)) {
    for (NTMMaterial mat : Mats.orderedList) {
        if (mat.autogen.contains(shape)) {
            ITEMS.register(shape.buildRegistryName(mat), () -> new Item(new Item.Properties()));
        }
    }
}
```

This is a much better fit than 188 individually hardcoded `DeferredItem` fields (which is what the
Neo Edition reference does for its equivalent per-material items - see `NtmItems.java`, e.g. the
~90 hardcoded `INGOT_*` fields) because CE's own design is already fully data-driven and regular
here: the compatibility rule (`autogen.contains(shape)`) is the single source of truth, materials
get added/removed from Mats.java over time, and a hardcoded field list would silently drift out of
sync. The only per-item customization CE has (the `.aot()` texture overrides and `bolt`'s `.oun()`)
is presentation-layer (texture path / translation key), not registration-shape, so it does not
argue for hardcoding the registration itself - it argues for a small override map/lookup consulted
during datagen (model/texture generation), separate from the registration loop.

A `Map<MaterialShapes, Map<NTMMaterial, ResourceLocation>>` (or a per-shape `Set` of "needs a
non-default model") is enough to carry the CE override list forward if pixel-parity textures are
wanted immediately; otherwise it can be deferred to whenever textures are actually authored, since
it affects datagen output, not the `DeferredRegister` calls.

Whether these should be plain `Item` or need a `hbm`-specific base class (CE's `ItemAutogen`
carries hazard-free, NBT-free "generic resource" semantics - no special behavior, no capabilities)
should be confirmed against the hazard registry area's findings, but nothing observed in
`ItemAutogen.java` itself suggests any of the 188 variants need behavior beyond a plain `Item`.

## 2. `ItemEnumMulti`-backed families (metadata-driven-multi via a Java `enum`, needs flattening)

`ItemEnumMulti<T extends Enum<T>>` is a second, unrelated generative mechanism: one `Item` whose
metadata damage value indexes into `T.values()`. Every one of these needs to become N distinct
registry entries post-flattening, one per enum constant. Found 21 fields in ModItems.java
(`ItemEnums.java`, plus two enums nested in `ItemDrive`/`ItemPWRFuel`):

| CE field | backing enum | values | count |
|---|---|---|---|
| `fuel_additive` | `EnumFuelAdditive` | ANTIKNOCK, DEICER | 2 |
| `drive` | `ItemDrive.EnumDriveType` | FLASH_EMPTY, DISK_EMPTY, FLASH_BROKEN, DISK_BROKEN, FLASH_FLIGHTSIM, FLASH_PARTICLESIM, DISK_FLIGHTDATA, DISK_FLIGHTDATA_PROCESSED, DISK_ORBITDATA, DISK_ORBITDATA_PROCESSED, KLAUS | 11 |
| `pwr_fuel_hot` | `ItemPWRFuel.EnumPWRFuel` | MEU, HEU233, HEU235, MEN, HEN237, MOX, MEP, HEP239, HEP241, MEA, HEA242, HES326, HES327, BFB_AM_MIX, BFB_PU241 | 15 |
| `pwr_fuel_depleted` | `ItemPWRFuel.EnumPWRFuel` (same enum, different item) | (same 15) | 15 |
| `pellet_rtg_depleted` | `EnumDepletedRTGMaterial` | BISMUTH, MERCURY, NEPTUNIUM, LEAD, ZIRCONIUM, NICKEL | 6 |
| `plant_item` | `EnumPlantType` | TOBACCO, ROPE, MUSTARDWILLOW | 3 |
| `casing` | `EnumCasingType` | SMALL, LARGE, SMALL_STEEL, LARGE_STEEL, SHOTSHELL, BUCKSHOT, BUCKSHOT_ADVANCED | 7 |
| `chunk_ore` | `EnumChunkType` | RARE, MALACHITE, CRYOLITE, MOONSTONE | 4 |
| `powder_ash` | `EnumAshType` | WOOD, COAL, MISC, FLY, SOOT, FULLERENE | 6 |
| `oil_tar` | `EnumTarType` | CRUDE, CRACK, COAL, WOOD, WAX, PARAFFIN | 6 |
| `briquette` | `EnumBriquetteType` | COAL, LIGNITE, WOOD | 3 |
| `coke` | `EnumCokeType` | COAL, LIGNITE, PETROLEUM | 3 |
| `circuit` | `EnumCircuitType` | 20 values (VACUUM_TUBE ... NUMITRON) | 20 |
| `parts_legendary` | `EnumLegendaryType` | TIER1, TIER2, TIER3 | 3 |
| `circuit_star_piece` | `ScrapType` | 22 values (BOARD_*/BRIDGE_*/CPU_*/MEM_*/CARD_*) | 22 |
| `circuit_star_component` | `CircuitComponentType` | CHIPSET, CPU, RAM, CARD | 4 |
| `page_of_` | `EnumPages` | PAGE1..PAGE8 | 8 |
| `achievement_icon` | `EnumAchievementType` | 10 values | 10 |
| `part_generic` | `EnumPartType` | PISTON_PNEUMATIC, PISTON_HYDRAULIC, PISTON_ELECTRIC, LDE, HDE, GLASS_POLARIZED | 6 |
| `item_secret` | `EnumSecretType` | CANISTER, CONTROLLER, SELENIUM_STEEL, ABERRATOR, FOLLY | 5 |
| `item_expensive` | `EnumExpensiveType` | 10 values | 10 |
| `ingot_metal` | `EnumIngotMetal` | SCRAP, INGOT, COUNTER, KEY, BEACON, CASING, CLOCKWORK, BAR, DETECTOR | 9 |

Total: 21 fields, 193 flattened variants (counting `pwr_fuel_hot`/`pwr_fuel_depleted` as separate
15-variant items since they are two different base items sharing one enum).

### Recommended port strategy

These are **not material-shape resources** in the `Mats`/`MaterialShapes` sense at all - the enum
values are arbitrary domain concepts (drive types, circuit tiers, casing sizes, ore chunk types),
not physical-form-of-a-material. Two sub-cases:

- Where the enum is small, closed, and unlikely to grow (`EnumFuelAdditive`, `EnumPlantType`,
  `EnumBriquetteType`, `EnumCokeType`, `CircuitComponentType`, etc.) - a short data-driven loop over
  `T.values()` inside this same item-registration file is simplest and keeps the single source of
  truth in the enum, exactly mirroring CE's own structure (CE already centralized the variant list
  in one enum instead of one-field-per-value, so this *is* CE's own DRY pattern - just needs the
  post-flattening translation from "one Item + metadata" to "N Items").
- `circuit` (20) and `circuit_star_piece`/`ScrapType` (22) are large enough, and different enough
  in per-value behavior (tiers, unlock progression, recipe roles), that whoever owns the "circuits"
  or "tech-tree progression items" Phase 1 area should confirm whether per-value behavior actually
  differs before deciding loop-vs-hardcode; from ModItems.java alone all 20/22 values look
  structurally identical (no per-constant fields on `EnumCircuitType`/`ScrapType`), so a loop is
  still the recommended default.

This area (`moditems_generative`) surfaces these because they are metadata-driven-multi per the
port's flattening rule, but the actual item enumeration/behavior work for `circuit`, `casing`,
`achievement_icon`, `page_of_`, `part_generic`, `item_secret`, `item_expensive`, `ingot_metal`,
`drive`, `pwr_fuel_*` more properly belongs to whichever Phase 1 area owns "machine/tech items",
"reactor fuel", "GUI/lore items", or "quest/achievement items" respectively - flagging the
boundary here rather than claiming ownership.

## 3. Large hand-coded, material-named families (irregular in CE, no shared base mechanism)

These are the families the task specifically asked to catalog: fields whose name embeds a material
name, with many near-identical siblings, but which CE implemented as individually hardcoded fields
(mostly `ItemCustomLore`, `ItemBakedBase`, occasionally `ItemHot`/`ItemUnstable`/`ItemFuel`/
`ItemLemon`/`ItemFoodBase`/`ItemSchraranium`) rather than through `ItemAutogen`. Exact counts via
`grep -oE "Item <prefix>[a-zA-Z0-9_]+ = new" ModItems.java | wc -l`:

| Prefix | Field count | Line range (approx) | Item classes used |
|---|---|---|---|
| `ingot_` | 112 | 780-886, 1005, 1136-1137, 1248*, 2801* | `ItemBakedBase`, `ItemCustomLore`, `ItemHot`, `ItemHotDusted`, `ItemUnstable`, `ItemSchraranium`, `ItemFuel`, `ItemFoodBase`, `ItemLemon` |
| `powder_` | 121 | 1009-1129, 1293-1294 | mostly `ItemBase`/`ItemCustomLore`, some `ItemFuel`/`ItemFertilizer`/`ItemLemon` |
| `nugget_` | 62 | 944-1006 | `ItemCustomLore` (majority), `ItemBakedBase`, `ItemUnstable` |
| `billet_` | 55 | 888-942 | `ItemCustomLore` (majority), `ItemBakedBase` |
| `plate_` | 37 | 1198-1204, 1249-1250*, 1725-1752 | `ItemBase` (majority), `ItemCustomLore`, `ItemBakedBase`, `ItemPlateFuel`, `ItemDepletedFuel` |
| `crystal_` / `gem_` | 33 + 5 | 1806-1840, 2334-2335, 2701-2702 | `ItemBase` (majority), `ItemCustomLore`, `ItemDrop`, `ItemFuel` |
| `waste_` | 16 | 1182-1197 | `ItemDepletedFuel` |
| `fragment_` | 9 | 1343-1351 | `ItemBase` |
| `stamp_` | 33 | 248-280 | `ItemStamp`, `ItemStampBook` (tool-crafting items, not resources - see section 4) |

(`*` = one or two entries in that range are the `ItemAutogen`/`ItemEnumMulti` fields already
covered in sections 1-2, not part of the hardcoded count logic, but grep's naive prefix match
picks up the field name text; the counts above are the literal grep counts including those, so
treat them as upper bounds - e.g. `ingot_` = 112 includes `ingot_raw` (section 1) and
`ingot_metal` (section 2) once each.)

Representative samples (not exhaustive - the full lists are trivially re-derived with the grep
above when implementing):

- `ingot_`: `ingot_steel`, `ingot_titanium`, `ingot_copper`, `ingot_uranium`, `ingot_u233/235/238`,
  `ingot_plutonium`, `ingot_pu238/239/240/241`, `ingot_am241/242`, `ingot_schrabidium`,
  `ingot_desh`, `ingot_uranium_fuel`, `ingot_thorium_fuel`, `ingot_mox_fuel`, ... down to exotic
  end-game materials (`ingot_euphemium`, `ingot_dineutronium`, `ingot_daffergon`).
- `nugget_` / `billet_`: mirror the `ingot_` material set closely (same radioactive/fuel materials
  get a nugget and a billet each), but neither list is a strict subset/superset of the other or of
  `ingot_` - e.g. `nugget_mercury` and `billet_zfb_bismuth`/`billet_zfb_pu241` have no `ingot_`
  counterpart, and several plain metals (bismuth, zirconium, niobium, beryllium, cadmium) have a
  nugget/billet but their "ingot" is instead produced generically via section 1's `ingot_raw`
  `ItemAutogen`-style entry or has no ingot at all.
- `powder_`: parallels `ingot_`/`nugget_`/`billet_` for smeltable metals, but also includes several
  `_tiny` variants (`powder_steel_tiny`, `powder_lithium_tiny`, `powder_boron_tiny`,
  `powder_niobium_tiny`, `powder_neodymium_tiny`, `powder_actinium_tiny`, `powder_lanthanium_tiny`,
  `powder_cerium_tiny`, `powder_coal_tiny`, `powder_meteorite_tiny`, `powder_iodine_tiny`,
  `powder_cobalt_tiny`, `powder_co60_tiny`, `powder_sr90_tiny`, `powder_at209_tiny`,
  `powder_pb209_tiny`, `powder_i131_tiny`, `powder_cs137_tiny`, `powder_xe135_tiny`,
  `powder_au198_tiny`) - these are the `DUSTTINY` shape's real backing items (`DUSTTINY` has a
  `Mats.java` autogen set of 6 materials, but as noted in the headline finding, no `ItemAutogen`
  call ever consumes it; CE hand-writes the `_tiny` powders instead, and inconsistently - some
  materials with `DUSTTINY` in their `Mats.java` autogen set have no `powder_*_tiny` field at all,
  and some `_tiny` fields exist for materials that do *not* have `DUSTTINY` in their autogen set.
  This is CE's own irregularity, not a porting artifact - flag rather than try to "fix" it.
- `plate_`: mix of genuinely generic plates (`plate_iron`, `plate_copper`, `plate_titanium` -
  plain `ItemBase`, no hazard behavior) and armor-insert plates (`plate_armor_titanium`,
  `plate_armor_ajr`, `plate_armor_hev`, `plate_armor_lunar`, `plate_armor_fau`,
  `plate_armor_dnt`) that are conceptually armor-crafting components, not material resources -
  worth double-checking with the armor-items area during implementation about which owns these.
- `crystal_`/`gem_`: almost entirely unique, flavorful items (`crystal_xen`, `crystal_horn`,
  `crystal_charred`, `gem_volcanic`) with no shared generation mechanism and no `Mats.java`
  `CRYSTAL`/`GEM`-shape autogen backing at all for most of them (only `MAT_EMERALD`, `MAT_SODALITE`,
  `MAT_CINNABAR` declare the `GEM` shape, corresponding to `gem_sodalite`/nothing named
  `gem_emerald` or `gem_cinnabar` directly - CE's gem_/crystal_ naming does not consistently track
  the `Mats.java` GEM autogen set either).
- `waste_`: `ItemDepletedFuel` reactor-waste counterparts to specific fuel ingots/plates
  (`waste_natural_uranium`, `waste_uranium`, `waste_thorium`, `waste_mox`, `waste_plutonium`,
  `waste_u233`, `waste_u235`, `waste_schrabidium`, `waste_zfb_mox`, plus `waste_plate_*` variants
  for plate-form fuels) - tightly coupled 1:1 to specific fuel items elsewhere in the file, not a
  generic "waste of any material" family.
- `fragment_`: a **separate, smaller ore-fragment family** (`fragment_neodymium`, `fragment_cobalt`,
  `fragment_niobium`, `fragment_cerium`, `fragment_lanthanium`, `fragment_actinium`,
  `fragment_meteorite`, `fragment_boron`, `fragment_coltan`) that coexists with, and is not the
  same item as, section 1's `ItemAutogen`-backed `bedrock_ore_fragment` (registry name
  `ore_fragment`). Some of its materials (neodymium, cobalt, niobium, boron, lanthanium) already
  have `FRAGMENT` in their `Mats.java` autogen set and thus already get a `bedrock_ore_fragment`
  variant too; others (`cerium`, `meteorite`, `coltan`) reference concepts with **no corresponding
  `NTMMaterial`/`Mats.java` entry at all**. This looks like a legacy/duplicate concept (possibly a
  pre-`ItemAutogen` ore-fragment mechanic that was partially superseded) - flag for the port lead
  to decide whether `fragment_*` is dead weight to drop or a distinct mechanic to preserve; do not
  assume it folds into the `bedrock_ore_fragment` family without confirming gameplay intent.

### Recommended port strategy for this family

**Individually hardcoded `DeferredItem` fields, not a generation loop**, for `ingot_`, `nugget_`,
`billet_`, `powder_`, `plate_`, `crystal_`/`gem_`, `waste_`, and `fragment_`. Reasoning:

- CE itself did not generalize these into any shared mechanism despite ~400 combined fields -
  strong evidence the naming/material-set is irregular enough (mismatched material sets between
  ingot/nugget/billet/powder, inconsistent `_tiny` coverage, armor-plate items mixed into `plate_`,
  gem/crystal items barely touching the `Mats.java` GEM shape at all) that a loop keyed off
  `Mats.orderedList` would either miss items CE has or invent items CE does not have.
- Several individual fields carry distinct behavior CE encodes as distinct Item subclasses
  (`ItemUnstable` for `ingot_u238m2`/`ingot_electronium`/`nugget_u238m2`, `ItemHot`/`ItemHotDusted`
  for `ingot_chainsteel`/`ingot_meteorite`/`ingot_steel_dusted`, `ItemSchraranium` for
  `ingot_schraranium`, `ItemFuel` for `ingot_graphite`/`ingot_c4`, `ItemFoodBase`/`ItemLemon` for
  `ingot_smore`/`ingot_semtex`/`powder_cement`) - a generic loop would need per-entry behavior
  overrides anyway, at which point the hardcoded-field approach is not meaningfully more code and
  is far easier to keep matched to CE's per-item hazard/tooltip/behavior data (see the hazard
  registry area's findings for which of these carry radiation/chem hazards via `ItemCustomLore`).
- This matches the Neo Edition reference's own approach (`NtmItems.java` hardcodes every
  `INGOT_*`/`NUGGET_*` field individually) - for once, that reference's design and CE's own
  regularity/irregularity assessment agree.

Two narrow exceptions worth a short helper (not a full generation loop) to cut boilerplate without
inventing structure CE doesn't have:
- A `registerNugget(name)` / `registerBillet(name)` / `registerIngot(name)` **helper function**
  (matching the Neo Edition reference's own `registerNugget(...)` helper at
  `NtmItems.java:130` onward) that just wraps `ITEMS.register(name, () -> new Item(new
  Item.Properties()))` for the plain (no special behavior) majority of each family, called once per
  field - keeps the per-field enumeration (correctness, matches CE's exact irregular set) while
  removing the `new Item(new Item.Properties())` repetition. This is a code-shape simplification,
  not a data-driven loop over `Mats.orderedList`.
- Do **not** attempt this simplification for the ~15-20% of each family's fields that need a
  distinct behavior class (`ItemUnstable`, `ItemHot`, `ItemDepletedFuel`, `ItemPlateFuel`,
  `ItemSchraranium`, `ItemFuel`, food items, etc.) - those stay explicit one-off registrations.

## 4. Items in this file that are NOT material-shape resources (belong to a different Phase 1 area)

ModItems.java is CE's single flat item dump, not organized by concern, so the bulk of its ~1800
fields are unrelated to material/shape generation. Grep-derived prefix families with 8+ members
that are clearly a different domain (spot-checked at least one representative field per group to
confirm the domain guess):

| Prefix | Count | Domain | Suggested owning area |
|---|---|---|---|
| `mp_` | 64 | Missile parts (`ItemMissile`, thrusters/warheads/fuselage) | weapons/missiles |
| `missile` | 42 | Assembled missiles | weapons/missiles |
| `battery` | 37 | Energy storage items (`ItemBattery`, `ItemSelfcharger`, `ItemFusionCore`) | energy/machine items |
| `upgrade_` | 36 | Machine upgrade items (`ItemMachineUpgrade`) | machine items |
| `stamp_` | 33 | Press-tool crafting stamps (`ItemStamp`) | tools |
| `pellet_` | 24 | Ammo pellets, RTG pellets (mixed: some are `ItemRTGPellet` resource-like, most are ammo) | weapons (ammo) / energy (RTG) - split by field, not a single family |
| `hazmat` | 22 | Hazmat armor pieces (`ArmorHazmat`) | armor |
| `ams` / `sat` | 22 + 20 | AMS/satellite system items | space/AMS systems |
| `warhead` | 19 | Missile warheads | weapons/missiles |
| `fluid` | 15 | Fluid container items | fluids/containers |
| `particle` / `flame` | 14 + 14 | Flamethrower/particle-weapon items | weapons |
| `part_` (non-generative ones, e.g. `part_lithium`, `part_beryllium`, `part_carbon`, `part_copper`, `part_plutonium`) | subset of 13 | Reactor/fusion component parts, one-off, not shape-driven | machine/reactor items |
| `gas_` | 12 | Gas canisters/masks | consumables/fluids |
| `syringe_` / `insert_` | 11 + 11 | Medical syringes, armor ballistic inserts | consumables / armor |
| `rod_` | 11 | Reactor fuel rods | reactor items |
| `steel_` / `cobalt_` / `titanium_` / etc. (material-prefixed but tool/armor, not resource) | ~9-14 each | Tool sets and armor sets built on `MaterialRegistry.aMat*`/`enumToolMaterial*` (`steel_helmet`, `steel_sword`, `steel_pickaxe`, `cobalt_decorated_axe`, ...) | tools / armor |
| `gun` | 8 | Firearms | weapons |
| `multitool` | 10 | Multitool variants | tools |

Also present but smaller/one-off, all clearly out of this area's scope: dosimeters/geiger
counters, screwdrivers/wrenches/drills (`ItemTooling`), pills/IVs/RadAway (`ItemPill`,
`ItemSimpleConsumable`), gas mask filters, FEL laser crystals (`laser_crystal_*`, 5 fields, keyed
by `EnumWavelengths` not `Mats`), fusion shields (`fusion_shield_*`, 4 fields), cladding/insert
armor components, minecart/train items, RBMK rod family (`rod_*` reactor control rods - distinct
from the `rod_` reactor-fuel-rod list above only in that both share the `rod_` prefix; they are
the same domain, reactor items), and the entire `ItemStamp`/`ItemStampBook` crafting-stamp family
(regular in structure - 4 stamp types x tiers - but a *tool*, not a resource, despite living next
to the resource items in the file).

None of these should be handled by this area (`moditems_generative`); they are flagged here only
because they physically live in the same file and a future implementer scanning ModItems.java
top-to-bottom needs to know where the resource-item boundary actually falls.

## Summary counts

- `ItemAutogen` (section 1): 17 fields, 188 generated (material, shape) variants, 108 distinct
  materials involved (a material typically appears in several shapes).
- `ItemEnumMulti` (section 2): 21 fields, 193 flattened variants needed post-metadata-removal.
- Hardcoded material-named resource families (section 3): ~440 individual fields across 9 prefix
  groups (`ingot_`, `powder_`, `nugget_`, `billet_`, `plate_`, `crystal_`/`gem_`, `waste_`,
  `fragment_`), recommended to stay individually hardcoded `DeferredItem` fields (optionally with a
  thin `registerX(name)` helper per family for the plain-behavior majority).
- Everything else in the 2855-line file (roughly 1150+ remaining fields) is out of scope for this
  area - tools, weapons/missiles, armor, food, energy storage, machine upgrades, reactor items, and
  other one-off special items belonging to other Phase 1 areas.
