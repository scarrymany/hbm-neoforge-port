# Creative tab population plan matching CE ordering

Research area: `creative_tabs_plan`. Read-only research for Phase 1 planning; no port project files
were modified.

## 1. What CE 1.12.2 actually does (confirmed by reading all 10 tab classes)

CE's ten tab classes (`com.hbm.creativetabs.{Parts,Control,Template,Resource,Block,Machine,Nuke,
Missile,Weapon,Consumable}Tab`) are almost all trivial `CreativeTabs` subclasses that only override
`createIcon()`. Only two carry extra behavior:

- **`ControlTab`** overrides `displayAllRelevantItems(NonNullList<ItemStack> list)`: after the
  superclass populates `list` normally, it finds every stack whose item is `IBatteryItem`, then
  replaces each with an explicit **full** copy and (only if `getChargeRate(stack) > 0`, i.e. not an
  SU-only battery) a preceding **empty** copy, mutating charge via `battery.setCharge(...)`.
- **`MissileTab`** overrides the same method to `list.add(...)` nine hand-built, hand-named
  `ItemCustomMissile.buildMissile(chip, warhead, fuselage, stability, thruster)` showcase stacks
  ("Lil Bub", "Uncle Kim", etc.) after the normal population.
- **`TemplateTab`** overrides `hasSearchBar()` (true) and `getBackgroundImageName()`
  (`item_search.png`); **`NukeTab`** overrides `getBackgroundImageName()` (`nuke.png`). Neither
  overrides item population.

Everything else about *which items land in which tab, and in what order*, comes from vanilla
`CreativeTabs.displayAllRelevantItems`'s default implementation: it walks the global item registry
and, for every item whose `getCreativeTab() == this`, calls `getSubItems(...)` (which is how
metadata-driven multi items expand into one stack per metadata variant) and appends the resulting
stacks to the tab's list, in registry-iteration order.

**Registry iteration order == item declaration order.** CE's items/blocks are
`public static final Item x = new XSubclass("x")....setCreativeTab(MainRegistry.xTab)`
field initializers that self-register via `GameRegistry.register` inside their constructors, in
top-to-bottom static-initializer order; Forge's `RegistryNamespaced`/`IdentityHashBiMap`-backed
registry preserves insertion order on iteration. So **CE's field order in `ModItems.java` /
`ModBlocks.java` *is* the creative-tab display order**, per tab.

Confirmed exact counts and pattern by grep:
- `ModItems.java`: **1484** occurrences of `.setCreativeTab(MainRegistry.<x>Tab)`.
- `ModBlocks.java`: **1084** occurrences of the same pattern.

So the task brief's hypothesis is confirmed as fact, not just "more likely": **CE's model is "every
item/block declares its own creative tab inline at its own declaration," not a per-tab
`getSubItems`/curated list** (except for the two documented special cases above).

Tab instantiation order (confirms Phase 0's tab order is exactly right), from
`MainRegistry.java`:

```
partsTab, controlTab, templateTab, resourceTab, blockTab, machineTab, nukeTab, missileTab,
weaponTab, consumableTab   (each via CreativeTabs.getNextID())
```

This matches Phase 0's `ModCreativeTabs` field order (`PARTS, CONTROL, TEMPLATE, RESOURCE, BLOCKS,
MACHINE, NUKE, MISSILE, WEAPON, CONSUMABLE`) and its `withTabsBefore` chain exactly - no changes
needed there.

Per-shape tab assignment for CE's metadata/programmatically-driven material items was spot-checked
directly in `ModItems.java`/`ModBlocks.java` and is consistent:

| CE shape (examples) | Tab |
|---|---|
| `ingot_*`, `plate_*`, (and by the same pattern: nugget/wire/billet/gem/crystal/dust/dense_wire/shell/pipe) | `partsTab` (**PARTS**) |
| `block_*` (metal storage blocks) | `blockTab` (**BLOCKS**) |
| `ore_*` | `resourceTab` (**RESOURCE**) |

This lines up with Phase 0's `MaterialShapes` design (`INGOT`, `PLATE`, `NUGGET`, `WIRE`, `BILLET`,
`GEM`, `CRYSTAL`, `DUST`, `DENSEWIRE`, `SHELL`, `PIPE`, `BLOCK`, ... - see
`com.hbm.inventory.material.MaterialShapes`), meaning **tab membership can be derived once per
`MaterialShapes` constant** rather than needing a manual per-material-per-shape entry.

## 2. Confirmed real NeoForge 21.1.228 API (verified against decompiled + NeoForge-patched source,
not guessed)

I could not find a `-sources.jar` for the joined, NeoForge-patched Minecraft classes on this
machine's usual Maven coordinates, but the project's own NeoGradle cache
(`~/.gradle/caches/neoformruntime/intermediate_results/`) still had the exact intermediate build
artifact for this project's own MC 1.21.1 + NeoForge 21.1.228 toolchain
(`sourcesAndCompiledWithNeoForge_...jar`, timestamped identically to the cached
`minecraft_1.21.1_client.jar`/`_server.jar` artifacts). I extracted and read
`net/minecraft/world/item/CreativeModeTab.java` from that exact jar. (A `mergeWithSources` jar in
the same directory turned out to be from a **different, newer, unrelated MC version cached
alongside 1.21.1** on this machine - I detected the mismatch because it used `Identifier` instead
of `ResourceLocation`, which contradicts Phase 0's own compiling `ModCreativeTabs.java`, and
excluded it. Flagging this here because it is exactly the kind of "don't invent/assume APIs"
trap the task warns about.)

Confirmed real shape of `CreativeModeTab.Builder` (NeoForge-patched, matches what Phase 0's
`ModCreativeTabs.java` already compiles against):

```java
CreativeModeTab.builder()
    .icon(Supplier<ItemStack>)
    .title(Component)
    .withTabsBefore(ResourceLocation...)              // or withTabsBefore(ResourceKey<CreativeModeTab>...)
    .backgroundTexture(ResourceLocation)               // real, matches Phase 0's javadoc reference
    .withSearchBar()                                   // NeoForge addition; also .withSearchBar(int width)
    .displayItems(CreativeModeTab.DisplayItemsGenerator)   // the population hook
    .build()

@FunctionalInterface
interface DisplayItemsGenerator {
    void accept(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output);
}

interface Output {
    void accept(ItemStack stack, TabVisibility visibility);
    default void accept(ItemStack stack);              // TabVisibility.PARENT_AND_SEARCH_TABS
    default void accept(ItemLike item, TabVisibility visibility);
    default void accept(ItemLike item);
    default void acceptAll(Collection<ItemStack> stacks, TabVisibility visibility);
    default void acceptAll(Collection<ItemStack> stacks);
}

enum TabVisibility { PARENT_AND_SEARCH_TABS, PARENT_TAB_ONLY, SEARCH_TAB_ONLY }
```

`buildContents(...)` is invoked by NeoForge's `BuildCreativeModeTabContentsEvent` machinery well
after mod construction/registration, so it is always safe for a `displayItems` lambda to reference
`DeferredItem`/`DeferredBlock` suppliers (via `.get()`) regardless of static-init ordering between
classes - by the time the lambda body actually runs, every `DeferredRegister` has already fired its
`RegisterEvent`.

There is also a NeoForge-only convenience overload:
`Builder.displayItems(Collection<? extends Holder<? extends ItemLike>> collection)`, explicitly
documented in its own javadoc as intended for `DeferredRegister#getEntries()`. **I evaluated and
rejected this for our design**: it dumps an entire `DeferredRegister`'s contents into one tab
unfiltered, which only works if each tab has its own dedicated `DeferredRegister`. Phase 0 already
committed to exactly one `ModItems.ITEMS` / `ModBlocks.BLOCKS` `DeferredRegister` shared across all
ten tabs' worth of content, so this overload is not applicable without abandoning that (already
shipped) structure - not recommended.

I confirmed this API shape is exactly what the Neo Edition reference's
`com.hbm.inventory.NtmCreativeTabs` already uses (`CreativeModeTab.builder()...displayItems((params,
output) -> { output.accept(...); ... })`), so it is a real, load-bearing pattern in this toolchain,
not a reference-only curiosity.

## 3. Neo Edition reference: useful for API shape, NOT a completeness or structure reference

`NtmCreativeTabs.java` (1325 lines) hand-lists every item with a bare `output.accept(NtmItems.X.get())`
call per line, grouped by tab, in one giant file - functionally the *inverse* of CE's per-item flag
(now the tab enumerates its members instead of items declaring their tab), which is unavoidable
given 1.21's `CreativeModeTab` has no per-item flag concept at all.

Important caveats found while reading it end-to-end that the task explicitly asked me to judge
independently rather than copy:

- **It only implements 8 of CE's 10 tabs.** `TEMPLATE` and `WEAPON` are both stubbed as a bare
  `/** SKIP */` comment with no registration at all - not present as tabs in Neo Edition.
- **It silently merges CE's `RESOURCE` (ore) tab into `BLOCKS`** - `NtmBlocks.ORE_URANIUM` and
  friends are listed inside the `"blocks"` tab alongside `BLOCK_STEEL` and other storage blocks,
  rather than as their own tab. This directly contradicts the confirmed CE source
  (`ore_uranium.setCreativeTab(MainRegistry.resourceTab)` vs.
  `block_steel.setCreativeTab(MainRegistry.blockTab)` - two different tabs in CE).
- It uses a small number of `addMetaItems(output, item)` / `addMaterialPartItems(output)` /
  `addMaterialControlItems(output)` / `addMaterialBlocks(output)` private helper methods to avoid
  fully flattening every call inline, but these are just manually-curated sub-lists, not a generic
  mechanism - every new item still needs a human to remember to add a line to the right helper.
- Fluid-container variants (tanks/barrels/packs, both plain and lead) are generated by a runtime
  loop over `Fluids.getInNiceOrder()`, which is a legitimate pattern worth reusing *if* Phase 1
  keeps a `FluidType`-keyed variant-stack item (component-based, not metadata) for these - it is the
  correct way to expand "1 registered item x many fluid variants" into many display stacks without
  hand-listing every fluid.

**Conclusion for our port:** use Neo Edition only to confirm the `CreativeModeTab.Builder` API shape
(confirmed above, independently, against the real compiled 1.21.1+NeoForge jar) and the
`addMetaItems`-via-component-expansion idea for the rare surviving multi-stack-per-item cases.
Do **not** copy its file structure, its 10->8 tab reduction, or its "one 1300-line manually
hand-typed file" approach - Phase 0 ground rules call for 99% parity (all 10 CE tabs, matching CE's
RESOURCE/BLOCKS split) and for tab population to not require a hand-maintained transcription that
drifts from the real item catalog.

## 4. Tension with Phase 0's own committed design, and my resolution

The task brief suggests, as an example: "a shared per-area list each item gets appended to,
consumed by `ModCreativeTabs` at BuildContents time" - i.e., item registration code in
`ModItems.java`/`ModBlocks.java` pushes itself into a tab-keyed list as a side effect of being
registered, mirroring CE's inline `.setCreativeTab(...)`.

I read Phase 0's actual `ModItems.java` before designing anything, and it already contains an
explicit, deliberate architectural decision that forecloses exactly that shape:

> "Creative tab placement is deliberately NOT part of this class: CE baked `setCreativeTab` into
> every item constructor call, but 1.21 assigns creative tab contents via a
> `BuildCreativeModeTabContentsEvent` listener owned elsewhere. Phase 1 item entries should not try
> to bolt a `setCreativeTab`-shaped API back onto `ItemBase`."

I am flagging this tension explicitly rather than silently picking a side, per the "no
placeholder/TODO judgments" ground rule. My reasoned recommendation, which honors Phase 0's
already-shipped boundary while still avoiding a "separate manual pass later": **split the problem by
provenance, not by trying to force one mechanism to cover both.**

### 4a. Material-shape-generated items (the ~1000+ item bulk: ingots/plates/nuggets/wires/blocks/
ores/etc. across every `Mats` x `MaterialShapes` combination)

These are not hand-written per-item declarations at all - they come out of one (or a handful of)
generation loop(s) that Phase 1 will write, iterating materials against `MaterialShapes` constants
and calling `ITEMS.registerItem(shape.buildRegistryName(mat), ...)` /
`BLOCKS.registerBlock(...)` in bulk. Because CE's shape-to-tab mapping is a small, fixed, and fully
consistent table (section 1 above), the *same generation loop* can look up
`tabFor(MaterialShapes shape)` from one small static table (add a `CreativeTabId tab` field, or a
side lookup map, to `MaterialShapes` itself, or keep the table entirely inside the creative-tabs
package if `MaterialShapes` should stay tab-agnostic) and append the freshly-created
`DeferredItem`/`DeferredBlock` supplier into a shared per-tab list, in the same statement that
registers it.

This is *not* "bolting a `setCreativeTab`-shaped API back onto `ItemBase`" (which would mean adding
a settable field/method to every item instance, resurrecting CE's per-item mutable state) - it's
one line inside one bulk-generation loop that already exists for a different reason (turning
`Mats x MaterialShapes` into registry entries) reading from one static shape->tab table. It fully
satisfies "doesn't require a separate manual pass later," because there is no per-item authoring
step for this class of item at all - it is automatic by construction, for every material added from
now on, forever.

### 4b. Hand-authored items/blocks (everything else: machines, tools, weapons, consumables,
ores by hardcoded id, missiles, decorative blocks, etc. - the CE items that do *not* come from the
`Mats x MaterialShapes` cross product)

For these, Phase 0's boundary stands: `ModItems.java`/`ModBlocks.java` stay pure registries with no
tab-shaped code. Tab membership for this class of item is declared where CE itself would recognize
it best: as a flat, ordered list of already-registered constants, grouped by tab, owned by the
`com.hbm.creativetabs` package (either inline inside `ModCreativeTabs.java`'s `displayItems`
lambdas, matching Neo Edition's confirmed-working shape, or in a small sibling class such as
`CreativeTabContents` if `ModCreativeTabs.java` would otherwise get too large - a real concern given
Neo Edition's equivalent file is 1325 lines). E.g.:

```java
// inside PARTS's displayItems, alongside the material-shape loop's output:
output.accept(ModItems.TURRET_MOB_FILTER.get());
output.accept(ModItems.CHEMISTRY_SET.get());
// ...
```

This is authored **at the same time** the item is added to `ModItems.java` - i.e. still "no
separate manual pass later" in the sense the task cares about (no deferred cleanup phase, no drift
window), even though it is a second file each new hand-authored item touches. This is the same
shape Phase 0 already accepted for hazards: `HazardRegistry.registerItems()` is documented as a
Phase 1 bulk table that references already-registered `ModItems` constants, built the same way CE's
own centralized `HazardRegistry.registerItems()` table works - i.e. CE itself uses *both* patterns
for different concerns (inline per-item flag for creative tabs; centralized bulk table for hazards),
and Phase 0 has already chosen the centralized-table shape for hazards. Doing the same for
hand-authored items' tab membership is consistent with that precedent, not a new pattern being
invented for this report.

**Net design:** `ModCreativeTabs`'s ten `displayItems` callbacks each do, in order:
1. Iterate that tab's slice of the auto-populated material-shape table (section 4a) - preserves
   registration order automatically, no manual work, ever.
2. `output.accept(...)` the tab's hand-authored constants (section 4b) - one line per item, added
   by whoever adds the item, in the same order CE declares them (so display order still matches CE)
   - and the two groups can interleave in whatever order best matches CE's real field order if a
   1:1 line-for-line ordering match ever matters (unlikely to matter much given CE mixes generated
   and hand-authored items freely in its own field order; a reasonable simplification is "generated
   materials first, then hand-authored items," which very closely approximates CE's own ordering
   in practice since materials dominate `PARTS`/`BLOCKS`/`RESOURCE` anyway - see the item counts in
   `ModItems.java`/`ModBlocks.java`).
3. Run the tab's bespoke post-processing, if any (see section 5).

### Concrete shared-list shape

```java
package com.hbm.creativetabs;

// one shared, mutable, tab-keyed list of item/block suppliers, built up during mod construction
// by (a) the material-shape generation loop(s) in ModItems/ModBlocks and (b) explicit calls
// listed inline in this class's own displayItems lambdas for hand-authored items.
final class CreativeTabContents {
    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<? extends ItemLike>>> BY_TAB =
            new HashMap<>();

    static void add(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
        BY_TAB.computeIfAbsent(tab, k -> new ArrayList<>()).add(item);
    }

    static void flush(ResourceKey<CreativeModeTab> tab, CreativeModeTab.Output output) {
        BY_TAB.getOrDefault(tab, List.of()).forEach(supplier -> output.accept(supplier.get()));
    }

    private CreativeTabContents() {}
}
```

`DeferredItem<Item>`/`DeferredBlock<Block>` both extend `DeferredHolder<R, T>`, which implements
`Supplier<T>`, and `Item`/`Block` both implement `ItemLike` - so `Supplier<? extends ItemLike>` is a
real, compiling type for both without any adapter code. `CreativeTabContents.add(...)` is called
from the material-shape generation loop (section 4a, referencing `MaterialShapes` -> tab lookup) and
is otherwise unused directly by `ModItems.java`/`ModBlocks.java` field declarations, preserving
Phase 0's stated boundary that those two classes stay pure registries.

## 5. Bespoke per-tab post-processing (does not fit the generic list model)

Two of CE's ten tabs need code beyond "flush the list," and Phase 0's own `ModCreativeTabs.java`
javadoc already correctly anticipated both (no change needed to that plan, just confirming it here
with the concrete mechanism):

- **CONTROL (battery full/empty split).** CE's `IBatteryItem`/charge-NBT concept becomes a data
  component in this port (per the ground rules: NBT -> Data Components). Whatever the Phase 1
  battery-item design lands on (a `ChargeComponent`-style record holding current/max charge, or
  similar), the equivalent of CE's post-processing is: after flushing CONTROL's generic list, walk
  it again (or maintain a separate small `List<Supplier<? extends ItemLike>>` of just the battery
  items, populated in 4a/4b like anything else) and, for each battery item, `output.accept(...)`
  a full-charge `ItemStack` and (only if the battery's charge rate is > 0, matching CE's SU-battery
  exception) an empty-charge `ItemStack`, using `ItemStack.set(componentType, chargeValue)` (or
  whatever the real Phase 1 battery API turns out to be) instead of CE's `battery.setCharge(stack,
  n)`. This is genuinely bespoke, cross-cutting logic (it operates on *stacks*, not on "does this
  item belong in this tab") and belongs as extra code inside CONTROL's `displayItems` lambda, run
  after the generic list flush - not something the generic per-item list mechanism should try to
  absorb.
- **MISSILE (nine curated showcase builds).** These are not registered items at all - they are
  `ItemStack`s built at display time from five *other* registered part items each (chip, warhead,
  fuselage, stability, thruster) via CE's `ItemCustomMissile.buildMissile(...)`, each given a custom
  colored display name. Whatever Phase 1's custom-missile-building item/component API turns out to
  be, this stays as nine explicit `output.accept(...)` calls (or one small `addShowcaseMissile(...)`
  private helper taking the five part suppliers plus a `Component` name/color) written directly
  inside MISSILE's `displayItems` lambda, exactly mirroring CE's `MissileTab.displayAllRelevantItems`
  override. Not a candidate for the generic list mechanism, and Neo Edition does not need to be
  consulted for this since it is pure CE business logic re-expressed for stacks/components instead
  of NBT.
- **TEMPLATE (search bar + background texture).** Confirmed real API:
  `CreativeModeTab.Builder.withSearchBar()` (sets `hasSearchBar` and, only if no custom background
  was already set, defaults the background to vanilla's own `item_search` texture) and
  `.backgroundTexture(ResourceLocation)` to point at the real ported `item_search.png` once that
  texture exists under this mod's namespace. `NUKE` needs only `.backgroundTexture(...)` pointed at
  the ported `nuke.png` (no search bar in CE's `NukeTab`). Both builder methods are confirmed against
  the real 1.21.1+NeoForge `CreativeModeTab.java` (section 2) - Phase 0's own javadoc reference to
  `CreativeModeTab.Builder#backgroundTexture` was already correct.

## 6. Summary checklist for Phase 1

1. Keep Phase 0's `ModCreativeTabs.java` tab set, order, and `withTabsBefore` chain exactly as-is
   (already matches CE's `MainRegistry.getNextID()` sequence 1:1).
2. Add a small `CreativeTabContents` (or equivalently-scoped) holder in `com.hbm.creativetabs`,
   keyed by `ResourceKey<CreativeModeTab>` (or an internal enum mirrored 1:1 to it), holding
   `List<Supplier<? extends ItemLike>>` per tab.
3. Wire the `Mats x MaterialShapes` bulk item/block generation loop(s) to call
   `CreativeTabContents.add(tabFor(shape), registeredSupplier)` using the shape->tab table in
   section 1 - this single change covers the large majority of CE's 1484+1084 `setCreativeTab`
   call sites with zero further manual work.
4. For hand-authored items/blocks, add one `output.accept(ModItems.X.get())` /
   `output.accept(ModBlocks.Y.get())` line inside the relevant tab's `displayItems` lambda in
   `ModCreativeTabs.java` at the same time the item is authored - keep `ModItems.java`/
   `ModBlocks.java` themselves free of any tab-shaped code, per Phase 0's own stated boundary.
5. Implement CONTROL's full/empty battery-stack post-processing and MISSILE's nine curated showcase
   stacks as explicit extra code inside those two tabs' `displayItems` lambdas, run after the
   generic list flush - ported from CE's two `displayAllRelevantItems` overrides, translated from
   NBT mutation to whatever data-component API Phase 1's battery/missile items land on.
6. Wire TEMPLATE's `.withSearchBar()` and TEMPLATE/NUKE's `.backgroundTexture(...)` once
   `item_search.png`/`nuke.png` are ported, per section 5 - both are confirmed-real
   `CreativeModeTab.Builder` methods, no further API research needed.
7. Do not port Neo Edition's `NtmCreativeTabs.java` file structure, its 8-tab reduction, or its
   fully-manual per-item listing style - use it only as already-confirmed evidence that the
   `CreativeModeTab.Builder.displayItems((params, output) -> ...)` shape genuinely compiles and
   runs against this toolchain.

## Files read for this research

- `com.hbm.creativetabs.ModCreativeTabs` (port, Phase 0)
- `com.hbm.items.ModItems`, `com.hbm.blocks.ModBlocks`, `com.hbm.hazard.HazardRegistry`,
  `com.hbm.main.MainRegistry` (port, Phase 0, for registration order and stated boundaries)
- `com.hbm.inventory.material.MaterialShapes` (port, Phase 0)
- `com.hbm.creativetabs.{PartsTab,ControlTab,TemplateTab,ResourceTab,BlockTab,MachineTab,NukeTab,
  MissileTab,WeaponTab,ConsumableTab}` (CE, full read of all 10)
- `com.hbm.items.ModItems`, `com.hbm.blocks.ModBlocks` (CE, targeted grep for `setCreativeTab`
  occurrence counts and shape/tab spot checks)
- `com.hbm.inventory.NtmCreativeTabs` (Neo Edition reference, full read, 1325 lines)
- `net.minecraft.world.item.CreativeModeTab` - extracted directly from this machine's own NeoGradle
  build cache for this exact toolchain (MC 1.21.1 + NeoForge 21.1.228,
  `neoformruntime/intermediate_results/sourcesAndCompiledWithNeoForge_...jar`, cross-checked against
  the `neoformruntime/artifacts/minecraft_1.21.1_*` jars by timestamp) to confirm the real
  `CreativeModeTab.Builder` API shape rather than relying on memory or the Neo Edition reference
  alone.
