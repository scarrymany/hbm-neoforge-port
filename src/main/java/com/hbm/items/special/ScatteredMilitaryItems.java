package com.hbm.items.special;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Item registration for {@code docs/phase3/scattered_military_items.md}'s remaining unrelated
 * singletons: {@link ItemDeadmanDetonator}/{@link ItemDeadMansExplosive} (CE's {@code ItemDrop}
 * detonator half only - the singularity/xen half is deliberately unowned, see the report's Deferred
 * scope), {@code cell_balefire} (CE: {@code ItemBakedBase}, confirmed unclaimed by any Phase 1/3 area
 * - a repo-wide grep found zero references anywhere in this port before this class), and the three
 * {@link ItemLootCrate} shells. New, separate registration class per this wave's established
 * one-file-per-package-slice convention (see {@code com.hbm.items.tool.DetonatorItems}).
 * <p>
 * Registry ids and tabs match CE's real {@code ModItems.java} declarations exactly (confirmed by
 * direct read): {@code detonator_deadman}/{@code detonator_de} -> CE's {@code nukeTab} (->
 * {@link ModCreativeTabs#NUKE}); {@code loot_10}/{@code loot_15}/{@code loot_misc} -> CE's
 * {@code missileTab} (-> {@link ModCreativeTabs#MISSILE}); {@code cell_balefire} -> CE's
 * {@code controlTab} (-> {@link ModCreativeTabs#CONTROL}).
 * <p>
 * <b>Not registered here</b> (per the report's explicit split): CE's {@code ItemDrop.beta} flavor
 * item (a two-line "silently vanish when dropped" rule with zero weapon behavior - belongs wherever
 * {@code ModItems.beta} is otherwise registered) and the entire singularity/xen half of
 * {@code ItemDrop} ({@code pellet_antimatter}, {@code singularity*}, {@code black_hole},
 * {@code capsule_xen}, {@code crystal_xen}) - unowned by any package this wave, depends on
 * {@code EntityVortex}/{@code EntityBlackHole}/{@code EntityRagingVortex}/{@code ExplosionChaos},
 * none of which exist in this port.
 */
public final class ScatteredMilitaryItems {

    private ScatteredMilitaryItems() {
    }

    public static final DeferredItem<ItemDeadmanDetonator> DETONATOR_DEADMAN =
            reg("detonator_deadman", () -> new ItemDeadmanDetonator(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ItemDeadMansExplosive> DETONATOR_DE =
            reg("detonator_de", () -> new ItemDeadMansExplosive(new Item.Properties().stacksTo(1)));

    /** CE: {@code new ItemBakedBase("cell_balefire").setContainerItem(ModItems.cell)} - plain reward material, no behavior of its own. */
    public static final DeferredItem<Item> CELL_BALEFIRE = reg("cell_balefire", () -> new Item(new Item.Properties()));

    public static final DeferredItem<ItemLootCrate> LOOT_10 =
            reg("loot_10", () -> new ItemLootCrate(new Item.Properties().stacksTo(1), ItemLootCrate.LIST_10));
    public static final DeferredItem<ItemLootCrate> LOOT_15 =
            reg("loot_15", () -> new ItemLootCrate(new Item.Properties().stacksTo(1), ItemLootCrate.LIST_15));
    public static final DeferredItem<ItemLootCrate> LOOT_MISC =
            reg("loot_misc", () -> new ItemLootCrate(new Item.Properties().stacksTo(1), ItemLootCrate.LIST_MISC));

    /** No-op body; referencing this class forces the static initializers above to run. */
    public static void registerAll() {
    }

    static {
        CreativeTabContents.add(ModCreativeTabs.NUKE, DETONATOR_DEADMAN);
        CreativeTabContents.add(ModCreativeTabs.NUKE, DETONATOR_DE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CELL_BALEFIRE);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, LOOT_10);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, LOOT_15);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, LOOT_MISC);
    }

    private static <T extends Item> DeferredItem<T> reg(String name, Supplier<T> factory) {
        return ModItems.ITEMS.register(name, factory);
    }
}
