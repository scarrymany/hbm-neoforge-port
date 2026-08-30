package com.hbm.creativetabs;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Shared, tab-keyed accumulator of item/block suppliers for creative-tab population.
 * <p>
 * CE assigns a creative tab to every item/block inline at the point of construction
 * ({@code setCreativeTab(MainRegistry.xTab)}); 1.21's {@link CreativeModeTab} has no per-item
 * flag, so tabs must instead enumerate their own contents from a {@code displayItems} callback
 * (see docs/phase1/creative_tabs_plan.md). This class is the enumeration side of that split:
 * <ul>
 *     <li>bulk material-shape generation loops (ingots/plates/ores/storage blocks/etc.) call
 *     {@link #add(ResourceKey, Supplier)} once per generated registry entry, looking up the
 *     target tab from a small {@code MaterialShapes -> tab} table;</li>
 *     <li>hand-authored items/blocks are added the same way, one call per item, written inline
 *     next to the {@code displayItems} lambda that owns their tab in {@link ModCreativeTabs}.</li>
 * </ul>
 * Every entry is registered before {@link ModCreativeTabs}'s {@code displayItems} callbacks ever
 * run, because those callbacks fire from {@code BuildCreativeModeTabContentsEvent}, well after
 * mod construction and all {@code DeferredRegister} registration.
 */
public final class CreativeTabContents {

    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<? extends ItemLike>>> BY_TAB =
            new HashMap<>();

    /**
     * Registers {@code item} to be displayed in {@code tab} once that tab's {@code displayItems}
     * callback flushes. Call order becomes display order within the tab.
     */
    public static void add(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
        BY_TAB.computeIfAbsent(tab, key -> new ArrayList<>()).add(item);
    }

    /**
     * Appends every item/block registered for {@code tab} (via {@link #add}) to {@code output},
     * in registration order. Safe to call multiple times or alongside additional bespoke
     * {@code output.accept(...)} calls in the same {@code displayItems} callback.
     */
    public static void flush(ResourceKey<CreativeModeTab> tab, CreativeModeTab.Output output) {
        BY_TAB.getOrDefault(tab, List.of()).forEach(supplier -> output.accept(supplier.get()));
    }

    private CreativeTabContents() {
    }
}
