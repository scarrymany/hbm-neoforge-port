# Phase 0 Research: `com.hbm.creativetabs`

## Scope

CE source (1.12.2/Forge): `hbm-ce/src/main/java/com/hbm/creativetabs/*.java` (10 files).
Registration/order call site: `hbm-ce/src/main/java/com/hbm/main/MainRegistry.java` lines 98-118.

## Class inventory

| File | CE base class | Purpose |
|---|---|---|
| `PartsTab.java` | `CreativeTabs` | Ingots, nuggets, wires, machine parts. Icon: `ModItems.ingot_uranium` (fallback `Items.IRON_PICKAXE`). |
| `ControlTab.java` | `CreativeTabs` | Items belonging in machines: fuels, cells, batteries, upgrades. Icon: `ModItems.pellet_rtg`. Overrides `displayAllRelevantItems` to post-process the tab's item list: for every item implementing `IBatteryItem` it removes the auto-generated stack and re-inserts an explicit "full charge" stack (always) and an explicit "empty charge" stack (only when `getChargeRate(stack) > 0`, i.e. skip empty variants for non-rechargeable "SU" batteries). |
| `TemplateTab.java` | `CreativeTabs` | Templates/blueprints, siren tracks. Icon: `ModItems.blueprints`. Overrides `hasSearchBar()` -> `true` and `getBackgroundImageName()` -> `"item_search.png"` (renders as the vanilla search tab with a magnifying-glass-style background instead of the normal tab background). |
| `ResourceTab.java` | `CreativeTabs` | Ore and mineral blocks. Icon: `ModBlocks.ore_uranium`. |
| `BlockTab.java` | `CreativeTabs` | Construction blocks. Icon: `Item.getItemFromBlock(ModBlocks.brick_concrete)` (note: the null-guard actually checks `ModBlocks.ore_uranium != null`, a copy-paste leftover from ResourceTab - the icon item itself is still `brick_concrete`). |
| `MachineTab.java` | `CreativeTabs` | Machines, multiblock structure parts. Icon: `Item.getItemFromBlock(ModBlocks.pwr_controller)`, no null-guard, annotated `@NotNull`. |
| `NukeTab.java` | `CreativeTabs` | Nuclear bombs. Icon: `Item.getItemFromBlock(ModBlocks.nuke_man)` (guarded on `ModBlocks.float_bomb != null`, another copy/paste mismatch between guard and returned item). Overrides `getBackgroundImageName()` -> `"nuke.png"` (custom tab background texture). |
| `MissileTab.java` | `CreativeTabs` | Missiles, satellites. Icon: `ModItems.missile_nuclear`. Overrides `displayAllRelevantItems` to append 9 hardcoded pre-built missile stacks via `ItemCustomMissile.buildMissile(chip, warhead, fuselage, stability, thruster)`, each with a colored custom display name (`TextFormatting.DARK_PURPLE`/`GREEN`/`BLUE` + name like "Lil Bub", "Long Boy", "Uncle Kim", etc). These are curated example missile loadouts assembled from `ModItems.mp_*` component items, not registered items themselves. |
| `WeaponTab.java` | `CreativeTabs` | Turrets, weapons, ammo. Icon: `ModItems.gun_vortex`. |
| `ConsumableTab.java` | `CreativeTabs` | Drinks, kits, tools. Icon: `ModItems.bottle_nuka`. |

All 10 classes share the same shape: a `(int index, String label)` constructor calling `super(index, label)`, and a client-only `createIcon()` override (`@SideOnly(Side.CLIENT)`) that null-checks a representative item/block from `ModItems`/`ModBlocks` and falls back to `Items.IRON_PICKAXE` if that content hasn't been registered yet (a load-order safety net that has no equivalent need in the ported architecture, since registration order is deterministic under `DeferredRegister`).

None of the 10 classes populate their own item list by default - Forge 1.12's `CreativeTabs` auto-populates from each item's `setCreativeTab(...)` call at item-registration time. Only `ControlTab` and `MissileTab` additionally override `displayAllRelevantItems` to mutate/append to that auto-populated list.

## Registration / ordering (MainRegistry.java:98-118)

```
partsTab, controlTab, templateTab, resourceTab, blockTab, machineTab, nukeTab, missileTab, weaponTab, consumableTab
```

10 tabs total, ids assigned via `CreativeTabs.getNextID()`, labels `"tabParts"`, `"tabControl"`, `"tabTemplate"`, `"tabResource"`, `"tabBlocks"`, `"tabMachine"`, `"tabNuke"`, `"tabMissile"`, `"tabWeapon"`, `"tabConsumable"` (these become `itemGroup.tabXxx` lang keys under CE's lang-key convention, i.e. `itemGroup.tabParts` etc. - confirm exact CE lang file key format when porting title translations, but the label string itself is the deterministic tab id/order-name to preserve).

## Cross-area dependencies

- `com.hbm.blocks.ModBlocks` (icons: `ore_uranium`, `brick_concrete`, `pwr_controller`, `nuke_man`, `float_bomb`) - owned by the blocks area, not in my scope.
- `com.hbm.items.ModItems` (icons: `bottle_nuka`, `pellet_rtg`, `ingot_uranium`, `blueprints`, `gun_vortex`, `missile_nuclear`, and the `mp_*` missile-part items used by `MissileTab`) - owned by the items area.
- `com.hbm.api.energymk2.IBatteryItem` (`ControlTab.displayAllRelevantItems`) - an item-capability interface owned by the API/energy area; needed only if `ControlTab`'s battery full/empty display logic is ported in Phase 1+.
- `com.hbm.items.weapon.ItemCustomMissile` (`MissileTab.displayAllRelevantItems`) - owned by the weapon-items area; needed only when the curated missile showcase entries are ported.
- `com.hbm.main.MainRegistry` - holds the 10 `public static CreativeTabs` fields and the order/labels above; this is the file that would need updating to point at the new tab registry, but `MainRegistry.java` is explicitly out of my edit scope per the task rules. My port plan below returns a `register(IEventBus)`-style entry point description instead of touching that file.
- Resource assets: `nuke.png` (custom tab background for `NukeTab`) and `item_search.png` (search-tab background for `TemplateTab`) - texture assets outside Java scope, needed if those two custom backgrounds are ported in a later phase (NeoForge does not have a simple `getBackgroundImageName()` equivalent - custom tab textures require a `CreativeModeTab` client-side rendering hook or are commonly dropped in ports that just use search-tab-style layout; flagged as an open question below).

## NeoForge 21.1 API confirmed via Neo Edition reference

`neo-edition/src/main/java/com/hbm/inventory/NtmCreativeTabs.java` shows the real, working pattern:

```java
public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NuclearTechMod.MODID);

public static final Supplier<CreativeModeTab> PARTS = CREATIVE_MODE_TABS.register(
        "parts",
        () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(NtmItems.INGOT_URANIUM.get()))
                .title(Component.translatable("itemGroup.parts"))
                .displayItems((itemDisplayParameters, output) -> {
                    output.accept(NtmItems.INGOT_URANIUM.get());
                    // ...
                }).build());

public static final Supplier<CreativeModeTab> CONTROL = CREATIVE_MODE_TABS.register(
        "control",
        () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(NtmItems.PELLET_RTG.get()))
                .withTabsBefore(NuclearTechMod.withDefaultNamespace("parts"))
                .title(Component.translatable("itemGroup.control"))
                .displayItems((itemDisplayParameters, output) -> { /* ... */ }).build());
```

Key API facts confirmed by this reference (real, in-use NeoForge 21.1 / MC 1.21.1 calls, Mojang mappings):
- Registry key is `net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB`.
- `DeferredRegister.create(ResourceKey<Registry<CreativeModeTab>>, String modid)` then `.register(String name, Supplier<CreativeModeTab>)`.
- `CreativeModeTab.builder()` fluent builder: `.icon(Supplier<ItemStack>)`, `.title(Component)`, `.displayItems(CreativeModeTab.DisplayItemsGenerator)`, `.withTabsBefore(ResourceLocation...)` for explicit ordering relative to other registered tabs, `.build()`.
- `displayItems` lambda receives `(ItemDisplayParameters, CreativeModeTab.Output output)`; items are added with `output.accept(ItemLike)`.
- Ordering between tabs in NeoForge is controlled with `.withTabsBefore(ResourceLocation)` / `.withTabsAfter(ResourceLocation)`, not an integer index like CE's `CreativeTabs.getNextID()`.
- `NuclearTechMod.withDefaultNamespace(String)` is their modid-scoped `ResourceLocation` helper - our port's equivalent would be our own mod's namespaced-ResourceLocation helper (e.g. `ResourceLocation.fromNamespaceAndPath(Tags.MODID, name)` or a similar helper already in `MainRegistry`/a `Tags`-like class, to be confirmed with the integration owner, since I must not edit `MainRegistry.java` myself).

## Recommended port plan

Create `com.hbm.creativetabs.ModCreativeTabs` (name chosen to mirror the existing `ModBlocks`/`ModItems` static-registry-holder convention used elsewhere in this codebase, and to avoid colluding with the Neo Edition's own `hbmsntm`-specific `NtmCreativeTabs` name):

```java
package com.hbm.creativetabs;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tags.MODID); // MODID constant TBD by integration owner

    public static final Supplier<CreativeModeTab> PARTS_TAB = CREATIVE_MODE_TABS.register(
            "tab_parts",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER)) // Phase 1: swap for real parts-tab icon item
                    .title(Component.translatable("itemGroup.hbm.tab_parts"))
                    .build());

    // ...same shape for control, template, resource, blocks, machine, nuke, missile, weapon, consumable,
    //    each chained with .withTabsAfter(previous tab's id) to preserve the exact CE ordering.

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
```

Design decisions:

1. **One `DeferredRegister<CreativeModeTab>` field per file is unnecessary** - CE's one-class-per-tab structure exists only because Forge 1.12 required a `CreativeTabs` subclass per tab to override `createIcon()`/`displayAllRelevantItems()`. NeoForge's builder pattern makes that subclassing moot; collapsing all 10 into one registrar class matches the Neo Edition's own `NtmCreativeTabs` precedent and avoids 10 near-empty files. Recommend deleting the 10 original per-tab classes' 1:1 file mapping and consolidating - this is the one place where I recommend deviating from "preserve package layout 1:1 file-for-file", since the *classes themselves* (`BlockTab`, `ConsumableTab`, etc.) are Forge-specific plumbing with no business logic to preserve, only icon-item choice and ordering, both of which carry over into the new single class's fields/comments.
2. **Registry ids**: use `tab_parts`, `tab_control`, `tab_template`, `tab_resource`, `tab_blocks`, `tab_machine`, `tab_nuke`, `tab_missile`, `tab_weapon`, `tab_consumable` (snake_case, mirroring CE's `tabXxx` label strings lowercased/underscored) so the full registry id is `hbm:tab_parts` etc. - legal under 1.21's registry-id charset and traceable back 1:1 to CE's field names (`partsTab` -> `tab_parts`).
3. **Ordering**: chain `.withTabsAfter(...)` (or `.withTabsBefore(...)`) through all 10 in the exact CE declaration order shown above (parts -> control -> template -> resource -> blocks -> machine -> nuke -> missile -> weapon -> consumable), each referencing the previous tab's `ResourceLocation`.
4. **Icons**: every tab gets `Items.BARRIER` as a placeholder icon for Phase 0, per the task's explicit instruction. Each tab's Javadoc/comment records which real item CE used as icon (`ModItems.pellet_rtg`, `ModBlocks.pwr_controller`, etc.) so Phase 1 can swap it in mechanically once that item/block exists in the port.
5. **Empty tabs**: no `.displayItems(...)` call at all in Phase 0 (an unset `displayItems` defaults to an empty generator), since item/block registration is out of scope. Do not call `.displayItems((params, output) -> {})` with an empty lambda body either - simply omit the call, which is cleaner and avoids a no-op lambda that reviewers would flag as dead code.
6. **`TemplateTab`'s `hasSearchBar()`**: `CreativeModeTab.Builder` in NeoForge 21.1 does not expose a builder method for this in the Neo Edition reference I found; if a genuine "search tab" behavior (search-bar-enabled tab background) is wanted later, it needs further research into `CreativeModeTab.Type` (there is a `CreativeModeTab.Type.CATEGORY`/`HOTBAR`/`SEARCH`/`INVENTORY` enum in vanilla 1.21 that the search tab specifically uses) - flagged as an open question, not resolved here since it has zero effect on an empty Phase-0 tab.
7. **`NukeTab`'s custom `nuke.png` background** and **`ControlTab`'s battery full/empty post-processing** and **`MissileTab`'s 9 curated missile showcase stacks**: all three are genuine business logic beyond "register an empty tab with an icon" and are explicitly out of Phase 0 scope (they depend on items/blocks/capabilities that don't exist yet). Flagged for Phase 1+ under "risks/open questions" below with the exact CE logic preserved above so a future agent can port it faithfully without re-reading CE.

## NBT / Data Components

No NBT reads/writes appear anywhere in the `creativetabs` package - none of these 10 classes touch `ItemStack` NBT. No NBT-key -> DataComponentType mapping is needed for this area.

## Risks / open questions

- **Copy-paste guard/icon mismatches in CE** (`BlockTab` guards on `ModBlocks.ore_uranium != null` but returns `brick_concrete`; `NukeTab` guards on `ModBlocks.float_bomb != null` but returns `nuke_man`): since Phase 0 discards all icon logic in favor of `Items.BARRIER`, these mismatches are moot for Phase 0, but Phase 1 should pick the *icon actually returned* (`brick_concrete`, `nuke_man`) as the real icon, not the item used only in the null-guard.
- **`ControlTab.displayAllRelevantItems` battery full/empty split** and **`MissileTab.displayAllRelevantItems` curated showcase stacks** require, respectively, an `IBatteryItem`-equivalent capability/interface and `ItemCustomMissile.buildMissile(...)` plus all nine `mp_*` component items to exist in the port first. Both are pure Phase 1+ (or later) concerns; no stub was written for them per the task's no-stub rule.
- **Custom tab background textures** (`nuke.png` for `NukeTab`, `item_search.png`/search-bar-tab for `TemplateTab`): NeoForge's `CreativeModeTab` builder (as exercised in the Neo Edition reference) does not show a background-texture override method. This needs targeted research into whether 1.21.1's creative tab rendering still supports a per-tab background image, or whether that visual gets dropped/simplified in the port. Not resolved in this research pass - flagged for whoever implements the tab-registration file, or for a follow-up client-rendering research task.
- **Translation keys**: CE's tab label strings (`"tabParts"` etc.) become lang keys the CE resource pack defines somewhere as `itemGroup.tabParts = ...`; I did not search the CE lang/assets files (out of my Java-source scope) to confirm the exact existing English text for each of the 10 tab titles. The implementer should pull the real strings from CE's `en_us.lang`/`en_us.json` (not invent new copy) when writing `Component.translatable("itemGroup.hbm.tab_xxx")` and the accompanying lang file entry.
- **`MainRegistry` wiring**: I did not edit `MainRegistry.java` per the task's hard rule. The integration owner should call `com.hbm.creativetabs.ModCreativeTabs.register(modEventBus)` from wherever `MainRegistry`'s `@Mod` constructor/event-bus setup lives, mirroring how the Neo Edition presumably wires `NtmCreativeTabs` into `NuclearTechMod`.
