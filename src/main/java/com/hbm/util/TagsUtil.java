package com.hbm.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Thin helper around the {@code minecraft:custom_data} component, which is where this port stashes
 * CE's ad-hoc NBT tags until each one is migrated to a proper vanilla or bespoke
 * {@link net.minecraft.core.component.DataComponentType}. {@link CustomData#copyTag()} always hands
 * back a defensive copy, so a tag obtained from {@link #getCustomData} must be written back through
 * {@link #putCustomData} to actually persist a mutation onto the stack.
 */
public class TagsUtil {

    public static boolean hasCustomData(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    public static void putCustomData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static CompoundTag getCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }
}
