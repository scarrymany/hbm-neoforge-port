package com.hbm.hazard.modifier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Level-modifying strategy, evaluated in a chain against a {@link com.hbm.hazard.HazardEntry}'s base level.
 */
public interface IHazardModifier {

    double modify(ItemStack stack, LivingEntity holder, double level);

    /**
     * Returns the level after applying all modifiers to it, in order.
     *
     * @param entity nullable
     */
    static double evalAllModifiers(final ItemStack stack, final LivingEntity entity, double level, final List<IHazardModifier> mods) {
        for (final IHazardModifier mod : mods) {
            level = mod.modify(stack, entity, level);
        }
        return level;
    }
}
