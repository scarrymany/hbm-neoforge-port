# Hazard bindings population plan for Phase 1 items

Scope: `com.hbm.hazard.HazardRegistry.registerItems()` and `registerContaminatingDrops()`, currently
empty no-ops in the port. This document is the concrete porting plan for populating them, built from
reading the full mechanism Phase 0 already shipped (`HazardSystem`, `HazardData`, `HazardEntry`,
`IHazardType`/`IHazardModifier` implementations) and CE's actual ~460-line binding table
(`upstream/hbm-ce/.../hazard/HazardRegistry.java`), which I read in full.

## 1. The mechanism Phase 0 already built (confirmed by reading the code, not assumed)

`HazardRegistry` in the port already carries over, verbatim, every constant and every
`IHazardType` singleton CE's table depends on: the isotope half-life-derived floats (`co60`, `sr90`,
`u235`, `pu239`, `wst`, `bf`, ...), the per-shape unit floats (`nugget`, `ingot`, `billet`, `rod`,
`rod_dual`, `rod_quad`, `rod_rbmk`, `block`, `powder`, `powder_tiny`, ...), and the `IHazardType`
singletons `RADIATION`, `CONTAMINATING`, `DIGAMMA`, `HOT`, `BLINDING`, `ASBESTOS`, `COAL`,
`HYDROACTIVE`, `EXPLOSIVE`, `TOXIC`, `COLD`. Nothing needs to be re-derived; Phase 1 just calls into
what's there.

`HazardSystem` (`src/main/java/com/hbm/hazard/HazardSystem.java`) exposes the real registration API,
already a faithful NeoForge translation of CE's engine:

```java
// tag-keyed, evaluated first
public static void register(TagKey<Item> tag, HazardData data)
public static void register(String tagName, HazardData data)          // resolves "namespace:path" -> itemTag(...)

// item-keyed, evaluated second
public static void register(Item item, HazardData data)
public static void register(Block block, HazardData data)              // uses block.asItem()
public static void register(ResourceLocation loc, HazardData data)     // resolves via BuiltInRegistries.ITEM immediately

// exact-stack-keyed, evaluated last (legacy-meta-equivalent via ComparableStack)
public static void register(ItemStack stack, HazardData data)          // ComparableStack(stack).makeSingular()
public static void register(ComparableStack comp, HazardData data)

// dispatches on runtime type of the key, useful for the same helper-method shapes CE used
public static void register(Object key, HazardData data)

public static void blacklist(Object keyOrTagOrCollection)              // exempts a key entirely
```

`HazardData` (mutable per-key bag) exposes exactly CE's builder shape:

```java
new HazardData()
    .addEntry(IHazardType hazard)                              // level = 1.0
    .addEntry(IHazardType hazard, double level)                 // silently no-ops for CONTAMINATING when RadiationConfig.enableContaminationOnGround is false - already ported, confirmed at HazardData.java:37
    .addEntry(IHazardType hazard, double level, boolean override)
    .addEntry(HazardEntry entry)                                 // for entries carrying an IHazardModifier
    .setMutex(int bits)
    .setOverride(boolean)
```

`HazardEntry` + `IHazardModifier` reproduce CE's modifier-chain entries one-for-one; the four modifier
classes CE's table needs already exist in the port with identical constructors:

- `new HazardModifierFuelRadiation(double target)` - generic durability-interpolated curve (used by CE's `registerOtherFuel`)
- `new HazardModifierRTGRadiation(double target)` - `ItemRTGPellet`-specific depletion curve (used by CE's `registerRTGPellet`)
- `new HazardModifierRBMKRadiation(double target, boolean linear)` - `ItemRBMKRod`/`ItemRBMKPellet`-specific curve, including xenon poisoning (used by CE's `registerRBMKRod`/`registerRBMKPellet`)
- `new HazardModifierRBMKHot()` - RBMK rod hull-heat -> HOT hazard (used by CE's `registerRBMKRod`)

`HazardTypeDangerousDrop(ObjDoubleConsumer<ItemEntity> onDrop)` also already exists, matching CE's
demon-core open->closed transition exactly (signature takes `(ItemEntity, double)` instead of CE's
custom functional interface, otherwise identical).

**Conclusion: zero new hazard-system plumbing is needed. Phase 1's job in this area is purely data
entry** - re-expressing CE's ~460-line table against the port's real item/tag identifiers.

## 2. CE's actual binding table, categorized by porting pattern

I read `upstream/hbm-ce/src/main/java/com/hbm/hazard/HazardRegistry.java` in full (681 lines). It
breaks into these patterns:

### Pattern A - direct single-item registration (majority of the table, ~120 calls)
`HazardSystem.register(someField, makeData(TYPE, level))` where `someField` is a plain
`public static Item`/`Block` field on CE's `ModItems`/`ModBlocks` with no metadata variants (e.g.
`ball_dynamite`, `nuclear_waste_long`, `ancient_scrap`, `block_corium`, `crystal_trixite`,
`gadget_core`, `boy_target`, `fleija_core`, `nuke_fstbmb`, `holotape_damaged`). Some entries carry
more than one hazard via chained `.addEntry(...)`, e.g.:
```java
HazardSystem.register(nuclear_waste_short, makeData().addEntry(RADIATION, 30F).addEntry(HOT, 5F));
```

**Port shape** - unchanged call shape, once the item field exists on the port's `ModItems`/`ModBlocks`:
```java
HazardSystem.register(ModItems.NUCLEAR_WASTE_LONG.get(), makeData(RADIATION, gen or literal level));
```
(`makeData(...)` stays as a private static helper on the port's `HazardRegistry`, copied verbatim from
CE - it's pure sugar over `new HazardData().addEntry(...)`, no NeoForge-specific change needed.)

This pattern is the bulk of the ~458-item count referenced in the Phase 0 handoff notes, and requires
no design decisions beyond "the item exists and its `DeferredItem`/registry-object accessor is named
X" - purely a Phase 1 items-area dependency, not a hazard-area design question.

### Pattern B - material-shape-driven fuel/waste item families (~40 calls)
Series like:
```java
HazardSystem.register(nugget_uranium_fuel, makeData(RADIATION, uf * nugget));
HazardSystem.register(billet_uranium_fuel, makeData(RADIATION, uf * billet));
HazardSystem.register(ingot_uranium_fuel, makeData(RADIATION, uf * ingot));
HazardSystem.register(block_uranium_fuel, makeData(RADIATION, uf * block));
```
repeated for uranium/plutonium/thorium/neptunium/mox/americium/schrabidium/hes/les "fuel" material
families, each item being a genuinely distinct, non-metadata registry entry per shape (these are
**not** the `ItemAutogen` metadata-multi items - CE gives each fuel shape its own field already, so no
expansion decision is needed here, just direct binding once each shape's port item exists).

**Port shape**: identical direct-item calls (falls under Pattern A mechanically), but *if* Phase 1's
material item generation ends up producing these "fuel" variants through the same per-material
`MaterialShapes` autogen mechanism as ordinary ingots/nuggets (see `Mats.java`/`MaterialShapes.java`),
the binding can instead be *table-driven* using the already-built naming convention:
```java
MaterialShapes.INGOT.buildRegistryName(Mats.MAT_URANIUM_FUEL) // -> "uranium_fuel_ingot" (illustrative; exact fuel-material identity/const name is a Phase 1 items-area decision, not resolved here)
```
This is a genuine open design question the items-area owns (whether "fuel" variants of uranium/
plutonium/etc. become their own `NTMMaterial` constants in `Mats.java`, or stay bespoke `ModItems`
fields as in CE) - the hazard binding itself is trivial either way (one `HazardSystem.register` call
per resulting item), so this plan does not block on it, but flags it as a dependency: whichever
naming Phase 1's items area lands on for the ~9 fuel-material families x ~4 shapes each (~36 items)
must be known before `registerItems()` can be filled in for this block.

### Pattern C - tag/ore-dict-string registration (~5 calls, plus the ore blacklist)
```java
HazardSystem.register("dustCoal", makeData(COAL, powder));
HazardSystem.register("dustTinyCoal", makeData(COAL, powder_tiny));
HazardSystem.register("dustLignite", makeData(COAL, powder));
HazardSystem.register("dustTinyLignite", makeData(COAL, powder_tiny));
...
for (String ore : TH232.all(MaterialShapes.ORE)) HazardSystem.blacklist(ore);
for (String ore : U.all(MaterialShapes.ORE)) HazardSystem.blacklist(ore);
```
CE's `HazardSystem.register(String tagName, ...)` resolved a bare Forge ore-dict name (e.g.
`"dustCoal"`) to an internal ore-dict tag key. The port's equivalent overload,
`HazardSystem.register(String tagName, HazardData)` (`HazardSystem.java:91`), calls
`ResourceLocation.parse(tagName)` - **a bare name with no colon is not a valid porting target for
this overload** (it would parse as `minecraft:dustcoal`, which is wrong and would not match the real
tag). This is a real divergence Phase 1 must handle explicitly, not by copying the CE call shape.

**Port shape** - build the proper `TagKey<Item>` via the naming convention Phase 0 already put on
`MaterialShapes`, and call the `TagKey<Item>` overload directly:
```java
HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_COAL), makeData(COAL, powder));
HazardSystem.register(MaterialShapes.DUSTTINY.commonTag(Mats.MAT_COAL), makeData(COAL, powder_tiny));
```
Note `MAT_COAL` in the port's `Mats.java` currently declares `setAutogen(FRAGMENT)` only (no `DUST`
shape) - confirmed by reading `Mats.java:71`. If dust items for coal/lignite are meant to exist as
real HBM items, the items area needs to add `DUST`/`DUSTTINY` to `MAT_COAL`/`MAT_LIGNITE`'s autogen
set; if instead this hazard binding is meant to catch *any* mod's coal dust sharing the convention
tag (mirroring CE's cross-mod ore-dict behavior, which this pattern was originally designed for -
OreDict entries were shared across all mods under the same string key), then registering against the
bare `c:dusts/coal` tag is actually correct and *more* faithful to CE's original intent than a
same-mod-only item binding would be, since NeoForge's `c:` convention tags are the direct successor
to Forge ore-dict names for exactly this cross-mod-sharing purpose. **Recommendation: register
against the tag, not a specific item**, for this exact reason - it needs no dependency on Phase 1
registering its own coal/lignite dust items at all, and correctly extends hazard coverage to any
compatible dust from other mods, matching CE's actual behavior.

The ore blacklist (`TH232`/`U` `.all(MaterialShapes.ORE)`) blacklists every registered ore-dict alias
of the ore block/item forms of thorium and uranium ore (raw ore blocks are not meant to be
radioactive in CE, only their processed forms are - the ore block itself is exempted). Port shape:
```java
HazardSystem.blacklist(MaterialShapes.ORE.commonTag(Mats.MAT_THORIUM));
HazardSystem.blacklist(MaterialShapes.ORE.commonTag(Mats.MAT_URANIUM));
```
**Caveat**: `MaterialShapes.ORE` is declared `.noAutogen()` with `registryName = null` (confirmed at
`MaterialShapes.java:49`), so `commonTag()` throws `IllegalStateException` for it by design (ore
shapes don't back a real autogenerated item/tag pair the same way). This blacklist call cannot use
`MaterialShapes.ORE.commonTag(...)` as written above - it needs whatever tag Phase 1's ore-block item
area actually assigns raw uranium/thorium ore blocks (likely a `c:ores/uranium` vanilla-convention
tag, not an HBM-specific `MaterialShapes` tag). This is a real open item for Phase 1 to resolve when
the ore blocks are registered - flagging it here rather than guessing the tag id.

### Pattern D - `ItemStack`(item, meta) exact-variant registration, from metadata-multi items (~35 calls)
CE frequently binds a *specific metadata value* of a still-flattened item, e.g.:
```java
HazardSystem.register(new ItemStack(sellafield, 1, 0), makeData(RADIATION, 0.5F));
HazardSystem.register(new ItemStack(sellafield, 1, 1), makeData(RADIATION, 1F));
... // 0..5
HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.NATURAL_URANIUM_FUEL.ordinal()), makeData(RADIATION, ...));
HazardSystem.register(new ItemStack(pellet_rtg_depleted, 1, EnumDepletedRTGMaterial.NEPTUNIUM.ordinal()), makeData(RADIATION, ...));
HazardSystem.register(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.RA226BE.ordinal()), makeData(RADIATION, ...));
HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.SCHRABIDIUM), makeData(RADIATION, ...));
```
These are exactly the "metadata-driven-multi" items the porting ground rules call out: post-flattening,
each `(item, meta)` / `EnumX.ordinal()` pair the items area expands into its own `DeferredItem<Item>`
becomes its own direct-item hazard binding (folds back into Pattern A). Concretely this affects, per
CE's table: `sellafield` (6 variants), `rod_zirnox_depleted` (9 variants via `EnumZirnoxTypeDepleted`),
`pellet_rtg_depleted` (1 used variant via `EnumDepletedRTGMaterial`), `ModItems.pile_rod` (8 variants
via `EnumPileRod`), `ModItems.watz_pellet` (10 variants via `ItemWatzPellet.EnumWatzType`),
`ModItems.pwr_fuel`/`pwr_fuel_hot`/`pwr_fuel_depleted` (14 variants each via `EnumPWRFuel`, handled by
the `registerPWRFuel` helper below), and `ModItems.rod`/`rod_dual`/`rod_quad` (14 `BreedingRodType`
variants each, handled by `registerBreedingRodRadiation` below), plus `holotape_image`'s
`EnumHoloImage.HOLO_RESTORED` variant.

**Port shape** - once each variant is its own item, this is just Pattern A, one call per resulting
item:
```java
HazardSystem.register(ModItems.SELLAFIELD_TIER_0.get(), makeData(RADIATION, 0.5F));
```
No `ComparableStack`/`ItemStack`-keyed registration should be needed for these once expansion happens
(the `register(ItemStack, ...)`/`register(ComparableStack, ...)` overloads exist in the port purely
for genuine still-ambiguous cases, if any remain, not for what were metadata variants). **This is the
single largest cross-dependency this hazard-binding work has on the items area**: `registerItems()`
cannot be written for any of these six families until the items area has committed to concrete field
names for every expanded variant.

### Pattern E - parametric helper methods wrapping a modifier chain (~90 calls total across all helpers)
CE factors repetitive modifier-chain bindings into small private helpers, all of which the port
should carry over near-verbatim since the modifier classes they use already exist:

```java
// depletion-curve fuel rods (uranium/plutonium/thorium/mox/... fuel plates and zirnox rods)
registerOtherFuel(Item fuel, float base, float target, boolean blinding)
registerOtherFuel(Item fuel, int meta, float base, float target, boolean blinding)   // meta variant -> Pattern D once expanded, drop the meta overload

// RTG pellets: base radiation curve keyed to ItemRTGPellet's own depletion fraction
registerRTGPellet(Item pellet, float base, float target[, float hot[, float blinding]])

// RBMK fuel rods/pellets: enrichment+xenon-driven radiation curve, optional hull-heat HOT entry
registerRBMKRod(Item rod, float base, float dep[, boolean linear | float blinding])
registerRBMKPellet(Item pellet, float base, float dep[, boolean linear[, float blinding, float digamma]])
registerRBMK(Item rod, float base, float dep, boolean hot, boolean linear, float blinding, float digamma)

// breeding rod: same base level scaled across 3 physical multiplicities (single/dual/quad),
// each a distinct metadata-expanded item -> becomes 3 direct Pattern-A calls per BreedingRodType
registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType type, float base)

// PWR fuel: same base level scaled 3 ways (fresh/hot/depleted), each its own metadata-expanded item
registerPWRFuel(EnumPWRFuel fuel, float baseRad)

// two-variant (fresh/depleted, or fresh/hot) waste item pairs sharing one base level
registerOtherWaste(Item waste, float base)                      // meta 0 = 0.075x base, meta 1 = base + HOT 5F
registerOtherWasteContaminating(Item waste, float base, float contaminating)   // adds CONTAMINATING to both
registerRadSourceWaste(Item waste, float base)                  // meta 0 = base, meta 1 = base + HOT 5F, no 0.075x scaling
```

**Port shape**: carry every one of these helpers over as private static methods on the port's
`HazardRegistry`, unchanged in body except:
1. Any `Item` parameter becomes whatever the items area's registry-object accessor type is (`Item`
   after `.get()`, or overload to accept `Supplier<? extends Item>`/`DeferredItem<Item>` directly if
   that reads better against the rest of the port's registration call sites - a stylistic call for
   whoever writes this file, not a functional one).
2. Any helper overload that took `int meta` to select a variant (`registerOtherFuel(Item, int meta, ...)`)
   must drop the meta parameter and instead take the already-resolved concrete `Item` for that
   variant, since the variant is now its own registry entry rather than a stack-meta pair.
3. The modifier classes (`HazardModifierFuelRadiation`, `HazardModifierRTGRadiation`,
   `HazardModifierRBMKRadiation`, `HazardModifierRBMKHot`) are used exactly as-is - already ported,
   already have the exact constructor shapes CE's helpers call.

Concrete example, `registerRBMKRod`, unchanged from CE except the signature note above:
```java
private static void registerRBMKRod(Item rod, float base, float dep) { registerRBMK(rod, base, dep, true, false, 0F, 0F); }

private static void registerRBMK(Item rod, float base, float dep, boolean hot, boolean linear, float blinding, float digamma) {
    HazardData data = new HazardData();
    data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierRBMKRadiation(dep, linear)));
    if (hot) data.addEntry(new HazardEntry(HOT, 0).addMod(new HazardModifierRBMKHot()));
    if (blinding > 0) data.addEntry(new HazardEntry(BLINDING, blinding));
    if (digamma > 0) data.addEntry(new HazardEntry(DIGAMMA, digamma));
    HazardSystem.register(rod, data);
}
```

### Pattern F - custom hazard-type instance inline (1 call)
```java
HazardSystem.register(demon_core_open, makeData(RADIATION, 5F).addEntry(new HazardTypeDangerousDrop((entityItem, level) -> {
    if (!entityItem.world.isRemote && entityItem.onGround) {
        entityItem.setItem(new ItemStack(ModItems.demon_core_closed));
        entityItem.world.spawnEntity(new EntityItem(entityItem.world, entityItem.posX, entityItem.posY, entityItem.posZ, new ItemStack(ModItems.screwdriver)));
    }
})));
```
**Port shape** - `HazardData.addEntry(IHazardType)` only exists for a bare `IHazardType` (level
defaults to 1.0), not an already-built `HazardEntry`; to inject a custom-typed entry at a non-default
level use `HazardData.addEntry(HazardEntry)` directly:
```java
HazardSystem.register(ModItems.DEMON_CORE_OPEN.get(), new HazardData()
    .addEntry(RADIATION, 5F)
    .addEntry(new HazardEntry(new HazardTypeDangerousDrop((entityItem, level) -> {
        if (!entityItem.level().isClientSide() && entityItem.onGround()) {
            entityItem.setItem(new ItemStack(ModItems.DEMON_CORE_CLOSED.get()));
            entityItem.level().addFreshEntity(new ItemEntity(entityItem.level(), entityItem.getX(), entityItem.getY(), entityItem.getZ(), new ItemStack(ModItems.SCREWDRIVER.get())));
        }
    }))));
```
(API renames applied here per confirmed 1.21 NeoForge/Mojang-mappings shapes already used elsewhere
in the port: `EntityItem` -> `ItemEntity`, `.world` -> `.level()`, `.isRemote` -> `.isClientSide()`,
`.posX/Y/Z` -> `.getX()/getY()/getZ()`, `world.spawnEntity(...)` -> `level().addFreshEntity(...)`.
`HazardTypeDangerousDrop`'s constructor already takes `ObjDoubleConsumer<ItemEntity>`, confirmed at
`hazard/type/HazardTypeDangerousDrop.java:25`, so the lambda shape above matches it directly.)

## 3. `registerContaminatingDrops()` plan

CE's version (must run after ore/dust items exist) layers a `CONTAMINATING` entry onto the
**already-registered** ore-dict dust hazard data via `computeIfPresent`, rather than replacing it:
```java
private static void addContaminatingDrop(final String ore, final double contaminating) {
    HazardSystem.oreMap.computeIfPresent(ore, (k, v) -> v.addEntry(CONTAMINATING, contaminating));
}
private static void addContaminatingDrop(final Item item, final double contaminating) {
    HazardSystem.itemMap.computeIfPresent(item, (k, v) -> v.addEntry(CONTAMINATING, contaminating));
}
```
The port's `HazardSystem` has no `oreMap` (CE's ore-dict-specific map) - the equivalent public map is
`HazardSystem.tagMap` (`Map<TagKey<Item>, HazardData>`, confirmed public at `HazardSystem.java:55`).
Both `tagMap` and `itemMap` are public static fields, so `computeIfPresent` can be called on them
directly from `HazardRegistry`, exactly as CE does.

**Port shape**:
```java
public static void registerContaminatingDrops() {
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_COLTAN /* or whatever the port names TCALLOY's material */), 0.07F * powder);
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_THORIUM), th232 * powder);
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_URANIUM), u * powder);
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_PLUTONIUM), pu * powder);
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_NEPTUNIUM), np237 * powder);   // MAT_NEPTUNIUM name unconfirmed, verify against Mats.java when written
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_POLONIUM), po210 * powder);
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_SCHRABIDIUM), sa326 * powder); // SA326 in CE is a schrabidium isotope, confirm exact Mats constant
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_STRONTIUM), sb * powder);       // CE's "SBD" - cross-check against strontium vs a distinct "SBD" material before binding
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_COBALT60), co60 * powder);
    addContaminatingDrop(MaterialShapes.DUSTTINY.commonTag(Mats.MAT_COBALT60), co60 * powder_tiny);
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_RADIUM), ra226 * powder);
    addContaminatingDrop(MaterialShapes.DUST.commonTag(Mats.MAT_ACTINIUM), ac227 * powder);
    addContaminatingDrop(MaterialShapes.DUSTTINY.commonTag(Mats.MAT_ACTINIUM), ac227 * powder_tiny);
    // ... AU198/GOLD198, SR90, AT209, PB209, I131, XE135, CS137 dust + dustTiny pairs, same shape
    addContaminatingDrop(ModItems.POWDER_BALEFIRE.get(), bf * powder);  // this one IS a concrete hbm item, not a shared tag - Pattern A style call
}

private static void addContaminatingDrop(final TagKey<Item> tag, final double contaminating) {
    HazardSystem.tagMap.computeIfPresent(tag, (k, v) -> v.addEntry(CONTAMINATING, contaminating));
}
private static void addContaminatingDrop(final Item item, final double contaminating) {
    HazardSystem.itemMap.computeIfPresent(item, (k, v) -> v.addEntry(CONTAMINATING, contaminating));
}
```
**Sequencing requirement carried over unchanged from CE**: this method must run after `registerItems()`
has populated `tagMap`/`itemMap` for the corresponding dust tags/items (`computeIfPresent` is a no-op
if the key isn't present yet) - Phase 0's own doc comment on `HazardRegistry.registerContaminatingDrops()`
already states this ("run after ore tags are registered"), and `MainRegistry`'s wiring instructions
(Phase 0 digest) already call `registerItems()` before `registerContaminatingDrops()` in the intended
common-setup sequence, so no new sequencing work is needed, just confirm the call order is preserved
when Phase 1 actually wires it in.

**Open item flagged, not resolved here**: several of CE's `OreDictManager` constants used in this
method (`TCALLOY`, `SBD`, `TS`) don't have an obviously-matching 1:1 `Mats.java` constant name from
what I sampled (I confirmed `MAT_RADIUM`, `MAT_ACTINIUM`, `MAT_SCHRABIDIUM`, `MAT_STRONTIUM` exist,
but did not exhaustively confirm every isotope material CE's table references, e.g. cobalt-60,
gold-198, tin/`TS`, tantalum-coltan-alloy/`TCALLOY`, antimony/`SBD`). Whoever writes
`registerContaminatingDrops()` for real must re-check each one against the finished `Mats.java` rather
than trust the illustrative constant names above.

## 4. Phase boundary: what belongs in Phase 1 vs. a later phase

Not every item CE's hazard table binds is in scope for "Phase 1: content mass (items and simple
blocks)". Splitting by dependency:

**Phase-1-appropriate now** (plain items/blocks with no bespoke behavior class, or simple
material-shape items): Pattern A entries whose backing item is a plain `Item`/`BlockItem` with no
custom logic (dynamite sticks, waste blocks/ingots/billets/nuggets, ore fragments, crystals, nuke
core/propellant items, debris items, sellafield-tier items once expanded, yellowcake, fallout,
asbestos brick, etc.), and all of Pattern B/C (material-shape and tag-driven dust/fuel items).

**Depends on a later phase's Item subclasses existing first** (Pattern E helpers and the metadata
families they bind in Pattern D): `ItemRTGPellet`, `ItemRBMKRod`, `ItemRBMKPellet`, `ItemZirnoxRod`/
`ItemZirnoxRodDepleted`, `ItemPWRFuel`, `ItemBreedingRod`, `ItemPileRodMK2`, `ItemWatzPellet` are all
stateful machine-fuel items (durability/enrichment/xenon/heat data-component-backed behavior) that
belong to whichever phase ports RBMK/RTG/PWR/breeder machine content, not to "simple items and
blocks." **Recommendation**: leave the corresponding `register*` helper calls out of Phase 1's first
pass at `registerItems()`; add them incrementally as each machine-item family gets ported in its own
phase, appending to the same `HazardRegistry.registerItems()` method (there is no reason to split this
into multiple methods - CE didn't, and `HazardSystem`'s maps accept registrations at any point before
common setup finishes). The `HazardModifierRTGRadiation`/`RBMKRadiation`/`RBMKHot`/`FuelRadiation`
classes and the `registerRTGPellet`/`registerRBMKRod`/`registerRBMKPellet`/`registerOtherFuel`/
`registerBreedingRodRadiation`/`registerPWRFuel`/`registerOtherWaste*`/`registerRadSourceWaste` helper
method bodies from section 2 (Pattern E) should still be written into the port's `HazardRegistry` as
part of documenting this plan's outcome, since they're pure translations with no forward dependency
on anything besides the `Item` fields they bind - only the *call sites* passing concrete items need to
wait.

## 5. Summary of concrete recommendations

1. Do not invent new hazard-system API - `HazardSystem.register(...)` (7 overloads),
   `HazardData`/`HazardEntry`/`IHazardModifier`, and all four modifier classes are complete and
   confirmed against Phase 0's actual source; Phase 1 only needs to call them.
2. Port CE's ~20 private helper methods (`makeData`, `registerOtherFuel`, `registerRTGPellet`,
   `registerRBMKRod`/`Pellet`/base `registerRBMK`, `registerBreedingRodRadiation`, `registerPWRFuel`,
   `registerOtherWaste`, `registerOtherWasteContaminating`, `registerRadSourceWaste`) into the port's
   `HazardRegistry` near-verbatim, dropping only the `int meta` overloads (metadata no longer exists).
3. Replace every CE `HazardSystem.register(String tagName, ...)` ore-dict-string call with
   `HazardSystem.register(MaterialShapes.<SHAPE>.commonTag(Mats.<MAT>), ...)`, using the naming
   convention Phase 0 already built for exactly this purpose - never re-parse a bare legacy ore-dict
   name as a `ResourceLocation`.
4. Every `new ItemStack(item, 1, meta)` / `EnumX.ordinal()` binding in CE's table (Pattern D, ~35
   calls across 6 item families: `sellafield`, `rod_zirnox_depleted`, `pellet_rtg_depleted`,
   `ModItems.pile_rod`, `ModItems.watz_pellet`, `ModItems.pwr_fuel*`, `ModItems.rod*`,
   `holotape_image`) becomes one direct-item `HazardSystem.register(Item, HazardData)` call per
   expanded variant, once the items area has committed to field names for each - this is this plan's
   single biggest external dependency and should be raised with whoever owns item-family expansion
   before `registerItems()` is written for real.
5. Populate `registerContaminatingDrops()` using `HazardSystem.tagMap`/`itemMap.computeIfPresent(...)`
   exactly as CE does with its `oreMap`/`itemMap`, run strictly after `registerItems()` in common
   setup (already the documented intended order from Phase 0's handoff notes) - do not merge this
   into `registerItems()` itself, since it depends on entries `registerItems()` already created.
6. Blacklisting raw uranium/thorium ore blocks from radiation needs the ore blocks' actual tag
   identity, which isn't decided yet - flag this as a Phase 1 ore-block follow-up, don't guess it now.
7. Keep the six machine-fuel-item helper families (RTG/RBMK/Zirnox/PWR/breeding-rod/pile-rod/Watz)
   out of Phase 1's first `registerItems()` pass; they depend on Item subclasses that belong to a
   later machine-content phase. Write their helper method bodies now (no forward dependency), call
   them once each item family is ported.
