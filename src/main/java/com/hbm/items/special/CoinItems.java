package com.hbm.items.special;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * CE coin items registration - simple lore items for easter eggs / rare drops.
 * CE ItemCustomLore pattern: setRarity(UNCOMMON/RARE) + creative tab.
 */
public final class CoinItems {

    private CoinItems() {
    }

    public static DeferredItem<Item> COIN_WORM;
    public static DeferredItem<Item> COIN_MASKMAN;

    public static void registerAll() {
        // CE coin_worm: ItemCustomLore, rarity=UNCOMMON, consumableTab
        COIN_WORM = ModItems.ITEMS.register("coin_worm", () ->
                new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, COIN_WORM);

        // CE coin_maskman: ItemCustomLore, rarity=UNCOMMON, consumableTab
        COIN_MASKMAN = ModItems.ITEMS.register("coin_maskman", () ->
                new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, COIN_MASKMAN);
    }
}
