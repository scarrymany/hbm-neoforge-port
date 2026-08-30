package com.hbm.items.tool;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Item registration for the Phase 2 energy cable/pylon network package's one required item
 * ({@code docs/phase2/energy_cable_pylon_network.md} - {@link ItemWiring}, needed in the same slice
 * as the pylon blocks or they would be permanently unlinkable). Separate top-level class (rather than
 * folding into {@code com.hbm.blocks.network.energy.EnergyNetworkBlocks}) so the one-line
 * {@code ModItems.register()} wiring call is self-contained, matching this port's block/item registry
 * split convention.
 */
public final class NetworkToolItems {

    public static DeferredItem<ItemWiring> WIRING_TOOL;

    private NetworkToolItems() {
    }

    public static void registerAll() {
        WIRING_TOOL = ModItems.ITEMS.register("wiring_tool", () -> new ItemWiring(new Item.Properties().stacksTo(1)));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, WIRING_TOOL);
    }
}
