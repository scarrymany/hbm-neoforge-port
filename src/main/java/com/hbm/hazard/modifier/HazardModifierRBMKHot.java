package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemRBMKRod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Converts RBMK rod hull heat into a HOT hazard level; overrides whatever base level was passed in.
 */
public class HazardModifierRBMKHot implements IHazardModifier {

    @Override
    public double modify(final ItemStack stack, final LivingEntity holder, double level) {

        level = 0;

        if (stack.getItem() instanceof ItemRBMKRod) {
            final double heat = ItemRBMKRod.getHullHeat(stack);
            level = Math.min(Math.ceil((heat - 100) / 10D), 60);
        }

        return level;
    }
}
