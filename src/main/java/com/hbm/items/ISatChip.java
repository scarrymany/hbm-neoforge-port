package com.hbm.items;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for satellite-frequency-chip items.
 *
 * CE stored the frequency as a raw "freq" int directly in the stack's NBT tag compound. Per the
 * data-components-not-NBT rule, this is backed by the dedicated hbm:sat_freq DataComponentType
 * (see HbmDataComponents.SAT_FREQ) instead.
 */
public interface ISatChip {

    static int getFreqS(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ISatChip satChip) {
            return satChip.getFreq(stack);
        }

        return 0;
    }

    static void setFreqS(ItemStack stack, int freq) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ISatChip satChip) {
            satChip.setFreq(stack, freq);
        }
    }

    default int getFreq(ItemStack stack) {
        return stack.getOrDefault(HbmDataComponents.SAT_FREQ.get(), 0);
    }

    default void setFreq(ItemStack stack, int freq) {
        stack.set(HbmDataComponents.SAT_FREQ.get(), freq);
    }
}
