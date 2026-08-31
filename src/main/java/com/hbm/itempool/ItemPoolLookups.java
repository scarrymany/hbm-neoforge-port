package com.hbm.itempool;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shared {@code BuiltInRegistries} lookup for Phase 8 {@code ItemPools*} files. Same remap as
 * {@link ItemPoolsLegacy}: CE prefix-first {@code block_*} → port autogen {@code *_block}.
 * Missing ids are skipped (debug log), never throw — RegisterEvent must have already fired.
 */
final class ItemPoolLookups {

    private ItemPoolLookups() {
    }

    static void add(ItemPool pool, String path, int min, int max, int weight) {
        Item item = lookup(path);
        if (item == null || item == Items.AIR) {
            MainRegistry.logger.debug("ItemPool: skip missing hbm:{}", path);
            return;
        }
        pool.pool.add(ItemPool.entry(item, min, max, weight));
    }

    static void addStack(ItemPool pool, ItemStack stack, int min, int max, int weight) {
        if (stack == null || stack.isEmpty()) return;
        pool.pool.add(ItemPool.entry(stack, min, max, weight));
    }

    static Item lookup(String path) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path)).orElse(null);
        if (item != null && item != Items.AIR) return item;
        if (path.startsWith("block_") && path.length() > 6) {
            String autogen = path.substring(6) + "_block";
            return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, autogen)).orElse(null);
        }
        return item;
    }
}
