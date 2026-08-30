# Phase 1 triage: `items/special` (43 files)

Survey of `com.hbm.items.special` (42 files) and `com.hbm.items.special.weapon` (1 file, `GunB92.java`,
counted in the same 43). This package is CE's grab-bag: it holds simple flavor items alongside items that are
functionally weapons, bombs, machine-interaction tools, and GUI-driven items. Findings below categorize every
file and call out cross-cutting systemic blockers that affect more than one file.

## Summary counts

- **Phase-1-safe (register as plain/simple items now):** 19
- **Phase-1-safe but needs metadata -> distinct-item flattening (ItemEnumMulti / hasSubtypes pattern):** 8
  (overlaps with some of the 19/GUI groups below where noted)
- **Needs deferral to a later phase / unbuilt system:** 10
- **Misfiled - should route to Phase 2 (machine-interaction):** 4
- **Misfiled - should route to Phase 3 (weapon/bomb/armor):** 5

(Categories overlap for a few files - e.g. an item can be both "metadata-multi" and "GUI-deferred" - so the
per-file table below is the authoritative breakdown, not the summary counts.)

## Cross-cutting systemic findings (read this before the per-file table)

### 1. `ItemEnumMulti<E>` / metadata-multi pattern must be flattened
`com.hbm.items.ItemEnumMulti<E extends Enum<E>>` (base class, not itself in `special/` but extended by five
files here) is a generic "one Item instance, N metadata variants keyed by `E.ordinal()`" pattern:
`hasSubtypes(true)`, `getSubItems` iterates the enum, `getTranslationKey(stack)` appends
`.<enumname>`, and texture/model resolution is keyed by metadata. This is exactly the flattening case called
out in the task ground rules. Every subclass of `ItemEnumMulti` found in this package needs to become N
distinct `DeferredItem` registry entries (one per enum constant) in the port:

- `ItemHoloTape<E>` - generic base, itself abstract-ish (no enum baked in).
- `ItemHolotapeImage` (`EnumHoloImage`, **18** constants) - GUI item, see finding 3.
- `ItemPlasticScrap` (`ScrapType`, **21** constants) - plain crafting-component family, straightforward flatten.
- `ItemSoyuz` (`ItemEnums.SoyuzSkinType`, **3** constants) - rocket/train skin selector, see finding 4.
- `ItemTrain` (`EnumTrainType`, **2** constants) - rail-car placer, see finding 4.

Two more files use ad-hoc (non-`ItemEnumMulti`) metadata-multi via a private `enum` + `setHasSubtypes(true)` +
manual `getSubItems`, which need the exact same flattening treatment even though they don't share the base
class:

- `ItemWasteLong` (`WasteClass`, **5** constants)
- `ItemWasteShort` (`WasteClass`, **8** constants)
- `ItemDepletedFuel` (2 raw metadata variants, no enum, just `0`/`1`)
- `ItemSiegeCoin` (metadata keyed to `SiegeTier.getLength()`, count not fixed at compile time - tier count is
  read from an external enum-like registry, confirm `SiegeTier` size before flattening)
- `ItemBedrockOreNew` - see finding 2, by far the largest expansion in this package.

`ItemAutogen` (finding 2) is the same idea again but material-driven instead of enum-driven, and is explicitly
what Phase 0's `MaterialShapes` naming convention already exists to replace - see below.

### 2. `ItemAutogen` and `ItemBedrockOreNew`: large generative item families, biggest scope items in this package
- **`ItemAutogen`** is CE's material-driven metadata-multi engine: one `Item` instance per *shape*
  (e.g. "ingot"), one metadata value per *material* that lists that shape in `mat.autogen`. This is precisely
  the old-CE mechanism that Phase 0's `MaterialShapes.buildRegistryName()`/`registryName`/`tagFolder`
  convention was built to replace. **Do not port `ItemAutogen` itself** - it should not survive into the port;
  its role is superseded by generating one `DeferredItem` per `(shape, material)` pair from
  `MaterialShapes`/`Mats.orderedList` directly, using the already-designed naming convention. This is the
  central mechanism for essentially all plain material items (ingots, nuggets, dusts, plates, etc.) and is
  almost certainly the single largest piece of Phase 1 work, but it is not "blocked" by anything - Phase 0
  built exactly what's needed to do it correctly. Flagging here only because the file *looks* like a normal
  special item but is actually the keystone architecture piece.
- **`ItemBedrockOreNew`** is a similar but independent generative system: `BedrockOreType` (6 ore-family
  buckets) x `BedrockOreGrade` (26 processing-stage grades) = **156 metadata combinations**, each with its own
  tint, texture overlay stack (`ProcessingTrait` layers baked via `ItemLayerModel`), and display name built
  from two i18n lookups. This needs its own explicit per-(type,grade) flattening design (156 distinct
  registry entries, or a strong argument for a smaller representation) before Phase 1 touches it - it is not
  reducible to the `MaterialShapes` convention since grades aren't materials. Its sibling `ItemBedrockOreBase`
  (the "raw scanned ore" item storing per-type float amounts in NBT) is comparatively simple and only needs the
  NBT->data-component conversion described in finding 5.

### 3. GUI/Menu framework not yet built - blocks several files
Nothing in the Phase 0 digest describes a ported Menu/Screen (`AbstractContainerMenu`/`Screen`) framework
equivalent to CE's `IGUIProvider` (`Container`/`GuiScreen` + `FMLNetworkHandler.openGui`/`player.openGui`).
That whole opening mechanism no longer exists in NeoForge 21.1 (replaced by `MenuProvider` +
`ItemStack`/`BlockEntity` menu-opening APIs) and needs its own design pass before any of these can be finished.
Files blocked on this:

- `ItemBook` (`ContainerBook`/`GUIBook`)
- `ItemBookLore` (`ContainerBook`-like/`GUIBookLore`, plus finding 2's dynamic-model problem)
- `ItemClayTablet` (`GUIScreenClayTablet`)
- `ItemHolotapeImage` (`GUIScreenHolotape`, plus finding 1's flattening)

Recommendation: register these items' shells now (they're otherwise simple), but defer wiring
`onItemRightClick`/menu-opening logic until the Menu/Screen framework exists. Do not block the whole item on
this - only the interactive-use behavior.

### 4. Rail/train and rocket-skin items depend on an unconfirmed vehicle subsystem
`ItemTrain` places `EntityRailCarBase` subclasses onto `IRailNTM` rail blocks (a full custom rail/train entity
system: gauges, anchors, yaw calculation). `ItemSoyuz` is a skin selector (`SoyuzSkinType`: normal/lunar/
post-war) for what is presumably a rocket or train-adjacent placeable structure - given the "post war"/"lunar"
skin descriptions this is very likely tied to a multiblock rocket/launch structure rather than a simple item.
Neither the rail/train entity system nor the rocket multiblock this feeds is mentioned as part of Phase 0.
Recommend deferring both until whichever phase owns vehicles/rail entities and rocket multiblocks is scoped -
this package is not the right place to make that call blind.

### 5. NBT -> Data Components inventory for this package
Concrete NBT keys found in this package that need Data Component treatment in the port:

| File | NBT key(s) | Notes |
|---|---|---|
| `ItemBedrockOreBase` | one double per `BedrockOreType.suffix` (6 keys) | small fixed-shape record component |
| `ItemBookLore` | `k` (String), `p` (short), `cov_col`/`tit_col` (int), `p1..pN` nested compounds each with `a1..aN` strings | structured "book content" component; nested per-page args need a list-of-records shape |
| `ItemFusionShield` | `damage` (long) | simple long component |
| `ItemKitCustom` / `ItemKitNBT` | arbitrary `ItemStack[]` via `ItemStackUtil` NBT helpers, `color1`/`color2` (int) | maps well to something like vanilla's `ItemContainerContents` component + two int/color components |
| `ItemDrop` (`detonator_deadman`) | `x`/`y`/`z` (int) | `BlockPos`-shaped component |
| `ItemTeleLink` | `x`/`y`/`z` (int) | same as above |
| `ItemClayTablet` | `tabletSeed` (long) | simple long component |
| `ItemHot` / `ItemHotDusted` | `heat` (int) | drives both gameplay (heat decay tick) and the render overlay - see finding 6 |
| `ItemPotatos` (battery) | `timer` (int, sound-cue cooldown) | separate from whatever charge component the Phase 0 energy API already defines for `ItemBattery` |
| `ItemUnstable` | `timer` (int) | decay countdown |
| `GunB92` (misfiled, Phase 3) | `animation` (int), `energy` (int) | noted for whoever picks up Phase 3 |

### 6. Legacy dynamic/baked-model system needs a full redesign, not a port
`ItemAutogen`, `ItemBedrockOreNew`, `ItemBookLore`, `ItemHot`/`ItemHotDusted`, and the `ItemEnumMulti` family
all use CE's runtime model system (`IModelRegister`/`IDynamicModels`/`IClaimedModelLocation`,
`IModel.retexture(...)`, hand-baked `IBakedModel`/`BakedQuad` with manual vertex-color patching for the "hot
glow" overlay in `ItemHot`). None of this API exists in modern Minecraft/NeoForge. Per the port's own ground
rule ("datagen, not hand-written JSON"), the replacement should be per-variant generated item model JSON
(datagen) rather than any runtime retexturing/baking mechanism. `ItemHot`'s alpha-blended glow overlay
specifically will need a modern equivalent - most likely an item model "range dispatch"/component-predicate
driven by the `heat` data component (finding 5) selecting between a cold and a glowing model, rather than
per-frame vertex color patching. `ItemSchraranium`'s `addPropertyOverride` (a removed 1.12 API) has the same
problem at smaller scale. This isn't a "defer to another phase" item since model/datagen work is explicitly
in Phase 1's own ground rules, but it is real, non-trivial design work that touches five files, so it's called
out here rather than repeated five times below.

### 7. `ItemModRecord` - verify against modern jukebox API before implementing
`ItemModRecord extends net.minecraft.item.ItemRecord` (1.12 API: item carries a `SoundEvent` directly). Modern
Minecraft (1.19.3+) replaced this with a `JukeboxSong` datapack registry entry plus a `JukeboxPlayable` data
component on the item - a materially different registration shape (song length, output range, comparator
output all data-driven). Do not assume the old constructor shape carries over; verify the exact 21.1 API
(check whether the Neo Edition reference has already added any custom records) before writing the port
version.

## Per-file table

Legend: **P1** = Phase-1-safe as-is (plain item, register now). **P1-flatten** = Phase-1-safe but is
metadata-multi and must become N distinct registry entries (see finding 1). **P1-defer(X)** = Phase-1-safe in
principle but blocked on system X. **-> Phase 2** = machine/multiblock-interaction item, misfiled here.
**-> Phase 3** = weapon/bomb/armor item, misfiled here.

| File | Category | Notes |
|---|---|---|
| `ItemAMSCore.java` | P1 | Reactor-fuel-core data item (power/heat/fuel stats baked into distinct instances, not metadata). Functionally meaningless without the fusion-reactor Phase 2 machine, but registers fine standalone. |
| `ItemAutogen.java` | **do not port** | Superseded by Phase 0's `MaterialShapes` per-material generation convention. See finding 2. |
| `ItemBedrockOreBase.java` | P1 | Simple item, NBT->component conversion needed (finding 5). Perlin-noise "ore scan" logic is pure math, ports directly. |
| `ItemBedrockOreNew.java` | P1-flatten (large) | 156 `(type, grade)` metadata combos + baked overlay layers. See finding 2 and finding 6. Largest single-file scope item in this package. |
| `ItemBook.java` | P1-defer(GUI/Menu framework) | See finding 3. |
| `ItemBookLore.java` | P1-defer(GUI/Menu framework) | See finding 3; also finding 5 (NBT) and finding 6 (dynamic model). |
| `ItemCell.java` | P1 - **but flag design ambiguity** | Metadata = arbitrary fluid ID (keyed off `Fluids`/`FluidType`, not a fixed enum). Flattening into "one item per fluid" would create dozens/hundreds of registry entries for what is conceptually a single container type. Recommend keeping this as **one** `hbm:cell` item with a data component holding the fluid's registry id (bucket/bundle-style), not per-fluid flattening - this is an explicit judgment call the ground rules ask to flag rather than punt on. |
| `ItemChopper.java` | P1-flatten (small) | 4 hardcoded mob-spawner variants (currently 4 separate `Item` instances distinguished by `this == ModItems.spawn_x`, not metadata - already effectively flattened, just confirm each stays a distinct registry id). Depends on entity classes (`EntityHunterChopper`, `EntityUFO`, `EntityBOTPrimeHead`, `EntityDuck`) existing - not this package's concern. |
| `ItemClayTablet.java` | P1-defer(GUI/Menu framework) | See finding 3; NBT seed -> component (finding 5). |
| `ItemConsumable.java` | P1 | Simple; real behavior routes through `ConsumableHandler` which itself needs porting (not blocking registration). |
| `ItemCustomLore.java` | P1 | Generic flavor/decorative item base (many discrete instances: runes, coins, eggs). `ScramblingName` Easter-egg ticker is pure logic, ports directly. Uses `ItemBakedBase` - see finding 6 only if any given instance overrides texture per-instance dynamically (most don't). |
| `ItemDemonCore.java` | P1 | Effectively empty/fully commented-out `ItemBase` subclass. Trivial. |
| `ItemDepletedFuel.java` | P1-flatten (small) | 2 raw metadata variants (fresh/cooling), color handler keyed to same. |
| `ItemDigamma.java` | P1 | Depends on `ContaminationUtil` (Phase 0 hazard system) - should already be available. |
| `ItemDoorSkin.java` | **-> Phase 2** | Reads/writes texture skin on a multiblock door's `TileEntity` (`BlockDummyable`/`IDoor`) - pure machine/multiblock-interaction item, not a "special" flavor item. |
| `ItemDrop.java` | **mixed - partially -> Phase 3** | Grab-bag of "bad things happen when dropped" items. `pellet_antimatter`, `singularity*`, `black_hole`, `capsule_xen`/`crystal_xen` are hazard/flavor drops (P1-safe, depend on entity/explosion effect classes existing). `detonator_deadman`/`detonator_de` explicitly interact with `IBomb` blocks and call bomb-explosion classes (`ExplosionLarge`) - this half of the file is weapon/bomb detonation hardware and belongs conceptually with Phase 3 bomb items, even though it's currently bundled in the same class as the harmless singularity drops. Recommend splitting the detonator behavior out when porting. |
| `ItemFuel.java` | P1 | Trivial (burn-time item, 2 instances referenced by identity). |
| `ItemFusionShield.java` | borderline **-> Phase 2** | Registers fine as a plain item, but is a consumable component of the fusion-reactor multiblock (`damage`/`maxTemp` tracked purely for that machine's benefit) - functionally a machine-interaction item per the ground rules. Recommend routing with Phase 2 fusion reactor work even though nothing stops registering it now. |
| `ItemGlitch.java` | P1 (registration only) | Single item, trivial to register. `onItemRightClick`'s 31-case random-effect table references dozens of items/blocks/entities spanning every future phase (missiles, nukes, treasure blocks, potions) - full behavior can only be completed once those dependencies exist across Phase 2/3. Register the item now; implement/port the effect table incrementally or last. |
| `ItemHoloTape.java` | P1-flatten | Generic empty base for enum-metadata holotapes (no enum of its own - see subclass). |
| `ItemHolotapeImage.java` | P1-flatten + P1-defer(GUI/Menu) | 18 `EnumHoloImage` variants (finding 1) and a GUI viewer (finding 3). Registration/flattening can proceed; the "view image" interaction waits on the Menu framework. |
| `ItemHot.java` | P1 | See finding 6 (dynamic model/glow overlay) and finding 5 (`heat` NBT->component). Logic (heat decay per tick) is simple once ported. |
| `ItemHotDusted.java` | P1-flatten (small) | Extends `ItemHot`; adds 10 metadata "forged" levels layered on top of the heat-overlay problem (finding 6). |
| `ItemKitCustom.java` | P1 | NBT->component conversion (finding 5): arbitrary contained `ItemStack[]` + 2 dye colors. |
| `ItemKitNBT.java` | P1 | Same NBT concern as `ItemKitCustom` (its superclass; see finding 5). |
| `ItemLootCrate.java` | **-> Phase 3 (content-dependent)** | Item registration itself is 3 trivial instances; but `list10`/`list15`/`listMisc` are populated with `ItemMissile` instances (weapon package) and the rarity-weighted roll only makes sense once missile items exist. Register the shell now; the loot table population necessarily waits on Phase 3 weapons. |
| `ItemModRecord.java` | P1 - **verify API** | See finding 7: modern jukebox/record API differs materially from 1.12's `ItemRecord`. Otherwise simple. |
| `ItemNuclearWaste.java` | P1 | Base class spawning a custom `EntityItemWaste` on drop; simple once that entity is ported (not this package's concern). |
| `ItemPlasticScrap.java` | P1-flatten | 21 `ScrapType` circuit-board-component variants (finding 1). Purely a crafting-component family, straightforward flatten, no other blockers. |
| `ItemPolaroid.java` | P1 | Trivial Easter-egg tooltip item keyed off a static `MainRegistry.polaroidID`. |
| `ItemPotatos.java` | borderline **-> Phase 2** | Extends `com.hbm.items.machine.ItemBattery` - it *is* a rechargeable HE-energy-API battery item (Fallout reference reskin) with one added sound-cue behavior. Belongs with Phase 2's battery items, not "special" flavor items, even though the class happens to live in this package. |
| `ItemRag.java` | P1 | Simple state-transition item (rag -> damp -> piss when wet/thrown). |
| `ItemSchraranium.java` | P1 | Simple, but uses the removed `addPropertyOverride` API (finding 6) for its config-conditional "nikonium" alt name/model. |
| `ItemSiegeCoin.java` | P1-flatten | Metadata count driven by `SiegeTier.getLength()` (an external tier list) rather than a fixed enum - confirm that count and whatever "siege" subsystem it belongs to before flattening; the item itself is simple. |
| `ItemSimpleConsumable.java` | P1 | Nice generic lambda-configurable base (`BiConsumer` use/hit actions) used to build many syringe/food items concisely - good pattern to preserve in the port. |
| `ItemSoyuz.java` | P1-flatten, **flag** | 3 `SoyuzSkinType` variants (finding 1); likely feeds a rocket/train multiblock placement rather than being purely decorative - see finding 4. Recommend confirming what this item actually places before committing to a design. |
| `ItemStarterKit.java` | P1 (registration only) | ~20 discrete kit items, trivial to register. `onItemRightClick`'s giveaway lists reference nuke machines/blocks (Phase 2), hazmat/power armor (Phase 3), missiles and grenades (Phase 3) almost exhaustively across the whole mod. Register the shells now; the actual item-giving behavior necessarily can't be completed until Phase 2 and Phase 3 land - recommend stubbing or building incrementally per kit as dependencies become available. |
| `ItemTeleLink.java` | **-> Phase 2** | Directly manipulates `TileEntityMachineTeleporter` (`ModBlocks.machine_teleporter`) - textbook machine-interaction item per the ground rules, not a flavor "special" item. |
| `ItemTrain.java` | P1-flatten, **defer** | 2 `EnumTrainType` variants (finding 1), but placement logic depends on a full custom rail/train entity subsystem (finding 4) not scoped in Phase 0. Recommend deferring until that subsystem's phase is decided. |
| `ItemUnstable.java` | **-> Phase 3** | This is a pocket nuke: on decay timer it spawns `EntityNukeExplosionMK5`, applies `ModDamageSource.nuclearBlast`, optional mushroom cloud. Pure bomb/weapon behavior; belongs with Phase 3 explosives, not "special" items. |
| `ItemWasteLong.java` | P1-flatten (small) | 5 `WasteClass` variants (finding 1). Meaningful only alongside Phase 2 breeder-reactor machinery, but registers standalone fine. |
| `ItemWasteShort.java` | P1-flatten (small) | 8 `WasteClass` variants (finding 1). Same notes as `ItemWasteLong`. |
| `weapon/GunB92.java` | **-> Phase 3 (misfiled)** | A full ranged weapon: charge-up mechanic, `EntityExplosiveBeam` projectile, nuke-if-overcharged easter egg, attribute modifiers, custom item model/animation state via NBT (`animation`, `energy` - finding 5). It even lives in a `special/weapon/` sub-package, confirming it was already recognized as misfiled within CE itself. Route entirely to Phase 3; nothing here belongs in Phase 1's item pass. |

## Recommendations for Phase 1 planning

1. Treat `ItemAutogen` as dead code to *not* port - its job is already superseded by Phase 0's
   `MaterialShapes` conventions. Build the material-item generation loop fresh from `Mats`/`MaterialShapes`
   instead of translating this class.
2. Scope `ItemBedrockOreNew`'s 156-variant flattening as its own explicit design task before touching it -
   it's the largest single expansion in this package and isn't reducible to the material-shape convention.
3. Stand up (or explicitly schedule) the Menu/Screen framework decision before `ItemBook`, `ItemBookLore`,
   `ItemClayTablet`, and `ItemHolotapeImage` can be finished - their item shells can register in the meantime.
4. Decide the model/datagen replacement strategy for the runtime-retexture pattern (finding 6) once, and
   apply it consistently to `ItemAutogen`'s replacement, `ItemBedrockOreNew`, `ItemBookLore`, `ItemHot`/
   `ItemHotDusted`, and any `ItemEnumMulti` subclass with `multiTexture = true`.
5. Route `ItemDoorSkin`, `ItemFusionShield`, `ItemPotatos`, `ItemTeleLink` to Phase 2 (machine-interaction),
   and `ItemDrop`'s detonator half, `ItemUnstable`, `weapon/GunB92.java` to Phase 3 (weapon/bomb), rather than
   porting them as part of the plain items/special sweep.
6. `ItemCell`'s "one item per fluid via metadata" pattern should almost certainly become "one item, fluid
   identity stored in a data component" rather than N flattened registry entries - flagging this explicitly
   since it cuts against the general flattening rule for a good reason (unbounded/extensible fluid list, not a
   fixed small enum).
