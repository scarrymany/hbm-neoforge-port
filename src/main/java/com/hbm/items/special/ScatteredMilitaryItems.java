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
 * detonator half only), {@code cell_balefire} (CE: {@code ItemBakedBase}, confirmed unclaimed by any
 * Phase 1/3 area - a repo-wide grep found zero references anywhere in this port before this class),
 * the three {@link ItemLootCrate} shells, and - per
 * docs/phase4/entities_vortex_gravity_wells.md, which found this exact gap and supplied the class to
 * fill it - the 8 {@link ItemDrop} singularity/xen/antimatter instances that were the other half of
 * CE's monolithic {@code ItemDrop} left unowned here until that package's own
 * {@code EntityVortex}/{@code EntityBlackHole}/{@code EntityRagingVortex}/{@code ExplosionChaos}
 * existed. New, separate registration class per this wave's established one-file-per-package-slice
 * convention (see {@code com.hbm.items.tool.DetonatorItems}).
 * <p>
 * Registry ids and tabs match CE's real {@code ModItems.java} declarations exactly (confirmed by
 * direct read): {@code detonator_deadman}/{@code detonator_de} -> CE's {@code nukeTab} (->
 * {@link ModCreativeTabs#NUKE}); {@code loot_10}/{@code loot_15}/{@code loot_misc} -> CE's
 * {@code missileTab} (-> {@link ModCreativeTabs#MISSILE}); {@code cell_balefire} and all 8
 * {@link ItemDrop} instances -> CE's {@code controlTab} (-> {@link ModCreativeTabs#CONTROL}). Stack
 * sizes match CE's real per-item {@code setMaxStackSize(1)} calls (present on 5 of the 8: both
 * singularities other than the two plain-vanilla-stack ones, {@code black_hole}, {@code singularity_spark},
 * {@code crystal_xen}; {@code capsule_xen}/{@code pellet_antimatter} keep the default stack size, CE
 * never restricts them). CE's {@code .setContainerItem(...)} calls on {@code singularity}/
 * {@code pellet_antimatter} (a crafting-remnant behavior) are not reproduced, matching this same
 * file's existing precedent for {@code cell_balefire}'s identical CE call.
 * <p>
 * <b>Not registered here</b>: CE's {@code ItemDrop.beta} flavor item (a two-line "silently vanish when
 * dropped" rule with zero weapon behavior - belongs wherever {@code ModItems.beta} is otherwise
 * registered).
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

    // --- CE's ItemDrop singularity/xen/antimatter half - see class javadoc ------------------------

    public static final DeferredItem<ItemDrop> SINGULARITY =
            reg("singularity", () -> new ItemDrop(new Item.Properties().stacksTo(1), ItemDrop.DropEffect.SINGULARITY));
    public static final DeferredItem<ItemDrop> SINGULARITY_COUNTER_RESONANT =
            reg("singularity_counter_resonant", () -> new ItemDrop(new Item.Properties().stacksTo(1), ItemDrop.DropEffect.SINGULARITY_COUNTER_RESONANT));
    public static final DeferredItem<ItemDrop> SINGULARITY_SUPER_HEATED =
            reg("singularity_super_heated", () -> new ItemDrop(new Item.Properties().stacksTo(1), ItemDrop.DropEffect.SINGULARITY_SUPER_HEATED));
    public static final DeferredItem<ItemDrop> SINGULARITY_SPARK =
            reg("singularity_spark", () -> new ItemDrop(new Item.Properties().stacksTo(1), ItemDrop.DropEffect.SINGULARITY_SPARK));
    public static final DeferredItem<ItemDrop> BLACK_HOLE =
            reg("black_hole", () -> new ItemDrop(new Item.Properties().stacksTo(1), ItemDrop.DropEffect.BLACK_HOLE));
    public static final DeferredItem<ItemDrop> CAPSULE_XEN =
            reg("capsule_xen", () -> new ItemDrop(new Item.Properties(), ItemDrop.DropEffect.CAPSULE_XEN));
    public static final DeferredItem<ItemDrop> CRYSTAL_XEN =
            reg("crystal_xen", () -> new ItemDrop(new Item.Properties().stacksTo(1), ItemDrop.DropEffect.CRYSTAL_XEN));
    public static final DeferredItem<ItemDrop> PELLET_ANTIMATTER =
            reg("pellet_antimatter", () -> new ItemDrop(new Item.Properties(), ItemDrop.DropEffect.PELLET_ANTIMATTER));

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
        CreativeTabContents.add(ModCreativeTabs.CONTROL, SINGULARITY);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, SINGULARITY_COUNTER_RESONANT);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, SINGULARITY_SUPER_HEATED);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, SINGULARITY_SPARK);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, BLACK_HOLE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CAPSULE_XEN);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CRYSTAL_XEN);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, PELLET_ANTIMATTER);
    }

    private static <T extends Item> DeferredItem<T> reg(String name, Supplier<T> factory) {
        return ModItems.ITEMS.register(name, factory);
    }
}
