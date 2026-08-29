package com.hbm.hazard.modifier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Generic durability-based depletion curve: interpolates the base radiation level toward {@link #target} as the
 * stack's damage value approaches its max damage.
 */
public class HazardModifierFuelRadiation implements IHazardModifier {

    private final double target;

    public HazardModifierFuelRadiation(final double target) {
        this.target = target;
    }

    @Override
    public double modify(final ItemStack stack, final LivingEntity holder, double level) {
        final double depletion = Math.pow(stack.getMaxDamage() == 0 ? 0D : stack.getDamageValue() / (double) stack.getMaxDamage(), 0.4D);
        return level + (this.target - level) * depletion;
    }
}
