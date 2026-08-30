package com.hbm.items.machine.rbmk;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The two plain-item RBMK fixtures this package needs that Phase 1 never registered: the depleted
 * fuel-rod casing (CE {@code ModItems.rbmk_fuel_empty}, {@link ItemRBMKRod}'s crafting remainder) and
 * the two removable core lids (CE {@code ModItems.rbmk_lid}/{@code rbmk_lid_glass}, read/placed by
 * {@code RBMKBaseBlock}'s screwdriver interaction - see that class's javadoc). Kept out of the
 * shared {@code ModItems.java}/{@code MachineItems.java} on purpose (many Phase 2 packages land this
 * same wave) - registered straight into the shared {@link ModItems#ITEMS} DeferredRegister, the same
 * pattern {@code PWRBlocks} uses for {@code ModBlocks.BLOCKS}.
 * <p>
 * <b>{@code RBMK_LID}/{@code RBMK_LID_GLASS} construct {@link ItemRBMKLid}</b> (updated by the Phase 2
 * machine-coupling items pass, {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}):
 * originally registered as plain {@link ItemBase} placeholders when this file first landed, since the
 * lid-install interaction {@code RBMKBaseBlock}'s own javadoc names them for was explicitly out of
 * this package's own scope ("not ported in this pass"). Only the {@code Item} subclass changed here -
 * the registry names, tab placement, and every other line are untouched.
 */
public final class RBMKItems {

    public static DeferredItem<Item> RBMK_FUEL_EMPTY;
    public static DeferredItem<Item> RBMK_LID;
    public static DeferredItem<Item> RBMK_LID_GLASS;

    private RBMKItems() {
    }

    public static void registerAll() {
        RBMK_FUEL_EMPTY = reg("rbmk_fuel_empty", () -> new ItemBase(new Item.Properties().stacksTo(1)));
        RBMK_LID = reg("rbmk_lid", () -> new ItemRBMKLid(new Item.Properties(), false));
        RBMK_LID_GLASS = reg("rbmk_lid_glass", () -> new ItemRBMKLid(new Item.Properties(), true));

        CreativeTabContents.add(ModCreativeTabs.CONTROL, RBMK_FUEL_EMPTY);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, RBMK_LID);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, RBMK_LID_GLASS);
    }

    private static DeferredItem<Item> reg(String name, java.util.function.Supplier<? extends Item> factory) {
        return ModItems.ITEMS.register(name, factory);
    }
}
