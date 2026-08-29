package com.hbm.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class WeightedRandomObject extends WeightedRandom.Item {

    Object item;

    public WeightedRandomObject(Object o, int weight) {
        super(weight);
        item = o;
    }

    public ItemStack asStack() {

        if (item instanceof ItemStack stack)
            return stack.copy();

        return null;
    }

    public Item asItem() {

        if (item instanceof Item i)
            return i;

        return null;
    }

    public String asString() {

        if (item instanceof String s)
            return s;

        return null;
    }
}
