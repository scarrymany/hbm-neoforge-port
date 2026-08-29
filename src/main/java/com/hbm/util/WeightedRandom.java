package com.hbm.util;

import net.minecraft.util.RandomSource;

import java.util.Collection;
import java.util.Iterator;

/**
 * CE's {@code net.minecraft.util.WeightedRandom} (1.7.10-era vanilla utility class) was removed from
 * Minecraft entirely by 1.21 in favour of {@code WeightedEntry}/{@code SimpleWeightedRandomList}.
 * This is a self-contained shim reproducing its exact old API (same {@code Item.itemWeight} field,
 * same static method names) so {@link WeightedRandomObject} and {@link WeightedRandomGeneric} need no
 * further changes, following the shim Neo Edition already vendored for the same purpose.
 */
public class WeightedRandom {

    /**
     * Returns the total weight of all items in a collection.
     */
    public static int getTotalWeight(Collection<? extends Item> items) {
        int i = 0;
        for (Item item : items) i += item.itemWeight;
        return i;
    }

    /**
     * Returns a random choice from the input items, with a total weight value.
     */
    public static Item getRandomItem(RandomSource random, Collection<? extends Item> items, int totalWeight) {
        if (totalWeight <= 0) {
            throw new IllegalArgumentException();
        }
        return getItem(items, random.nextInt(totalWeight));
    }

    //Forge: Added to allow custom random implementations, Modder is responsible for making sure the
    //'weight' is under the totalWeight of the items.
    public static Item getItem(Collection<? extends Item> items, int weight) {
        int j = weight;
        for (Item item : items) {
            j -= item.itemWeight;
            if (j < 0) return item;
        }
        return null;
    }

    /**
     * Returns a random choice from the input items.
     */
    public static Item getRandomItem(RandomSource random, Collection<? extends Item> items) {
        return getRandomItem(random, items, getTotalWeight(items));
    }

    public static class Item {
        /** The Weight is how often the item is chosen(higher number is higher chance(lower is lower)) */
        public int itemWeight;

        public Item(int itemWeight) {
            this.itemWeight = itemWeight;
        }
    }
}
