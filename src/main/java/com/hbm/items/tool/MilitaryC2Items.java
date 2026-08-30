package com.hbm.items.tool;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Item registration for {@code docs/phase3/scattered_military_items.md}'s Cluster 1 (military C2
 * items): {@link ItemRadarLinker}, {@link ItemRangefinder} (+ its flattened polarized variant),
 * {@link ItemRTTYPager}, {@link ItemAmatExtractor}. New, separate registration class - same
 * one-call-from-{@code ModItems.register()} pattern as {@code DetonatorItems}/{@code NetworkToolItems}.
 * <p>
 * Registry ids and creative tabs match CE's real {@code ModItems.java} declarations exactly
 * (confirmed by direct read): {@code radar_linker}/{@code rtty_pager} -> CE's {@code consumableTab}
 * (-> {@link ModCreativeTabs#CONSUMABLE}); {@code rangefinder} -> CE's {@code missileTab}
 * (-> {@link ModCreativeTabs#MISSILE}); CE's {@link ItemAmatExtractor} field is actually named
 * {@code bismuth_tool} in {@code ModItems.java} (its constructor's own {@code "s"} parameter is the
 * real registry id, not the Java field name) -> CE's {@code consumableTab}. {@code rangefinder_polarized}
 * is a new id this port invents (see {@link ItemRangefinder}'s javadoc) - not fixed by any CE source,
 * placed in the same tab as its base item.
 */
public final class MilitaryC2Items {

    private MilitaryC2Items() {
    }

    public static final DeferredItem<ItemRadarLinker> RADAR_LINKER =
            reg("radar_linker", () -> new ItemRadarLinker(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<ItemRangefinder> RANGEFINDER =
            reg("rangefinder", () -> new ItemRangefinder(new Item.Properties().stacksTo(1), false));
    public static final DeferredItem<ItemRangefinder> RANGEFINDER_POLARIZED =
            reg("rangefinder_polarized", () -> new ItemRangefinder(new Item.Properties().stacksTo(1), true));

    public static final DeferredItem<ItemRTTYPager> RTTY_PAGER =
            reg("rtty_pager", () -> new ItemRTTYPager(new Item.Properties().stacksTo(1)));

    /** CE field name {@code bismuth_tool} (see class javadoc) - registry id kept identical to CE. */
    public static final DeferredItem<ItemAmatExtractor> BISMUTH_TOOL =
            reg("bismuth_tool", () -> new ItemAmatExtractor(new Item.Properties().stacksTo(1)));

    /** No-op body; referencing this class forces the static initializers above to run. */
    public static void registerAll() {
    }

    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, RADAR_LINKER);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, RANGEFINDER);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, RANGEFINDER_POLARIZED);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, RTTY_PAGER);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, BISMUTH_TOOL);
    }

    private static <T extends Item> DeferredItem<T> reg(String name, Supplier<T> factory) {
        return com.hbm.items.ModItems.ITEMS.register(name, factory);
    }
}
